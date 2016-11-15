package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import interfaces.IPubSubbable;
import interfaces.IShutdownable;


public class Parser<ReceiveType, PublishType> implements IShutdownable{
	
	//Fields.  Organized in builder class
	private final HashMap<String, Receiver<ReceiveType>> map;	
	private final LinkedBlockingQueue<MessageTuple<PublishType>> sender;
	private final ExecutorService executor; //4 threads, just to make sure
	private final Vector<Future<?>> futures;	
	private final IPubSubbable<ReceiveType, PublishType> pubsubInterface;
	public final Metrics metrics;

	private Parser(ParserBuilder<ReceiveType, PublishType> builder) {
		this.map = builder.map; 
		this.sender = builder.sender; 
		this.futures = builder.futures;
		this.executor = builder.executor;
		this.pubsubInterface = builder.pubsubInterface;
		this.metrics = builder.metrics;
	}

	public MessageTuple<ReceiveType> getMessageBlocking(String channel) throws InterruptedException {
		return map.get(channel).receiver.take();
	}
	
	public MessageTuple<ReceiveType> getMessage(String channel) {
		return map.get(channel).receiver.poll();
	}
	
	public MessageTuple<ReceiveType> getMessageTimeout(String channel, int milliseconds) throws InterruptedException {
		return map.get(channel).receiver.poll(milliseconds, TimeUnit.MILLISECONDS);
	}
	
	public void sendMessage(String channel, PublishType message) throws InterruptedException {
		
		MessageTuple<PublishType> m = new MessageTuple<PublishType>(channel, message);
		
		//TODO: fix this... sender.put(m);
		
		this.pubsubInterface.publish(m);
	}
	
	/**
	 * 
	 * <p>  Function to request coordinate between multiple channels.  Can send a "request" message on one channel.  Then, it will block until
	 * a message is received on the "receive" channel.
	 * 
	 * @param reqChannel The channel on which data is requested
	 * @param message Message sent on reqChannel
	 * @param recChannel Channel on which data is received
	 * @return The received message
	 * @throws InterruptedException
	 */
	public MessageTuple<ReceiveType> coordinateBlocking(String reqChannel, PublishType message, String recChannel) throws InterruptedException {
		this.sendMessage(reqChannel, message);
		return this.getMessageBlocking(recChannel);
	}
	
	/**
	 * 
	 * <p>  Function to request coordinate between multiple channels.  Can send a "request" message on one channel.  Then, it will blocking for "milliseconds."
	 * 
	 * @param reqChannel The channel on which data is requested
	 * @param message Message sent on reqChannel
	 * @param recChannel Channel on which data is received
	 * @return The received message
	 * @throws InterruptedException
	 */
	public MessageTuple<ReceiveType> coordinateTimeout(String reqChannel, PublishType message, String recChannel, int milliseconds) throws InterruptedException {
		this.sendMessage(reqChannel, message);
		return this.getMessageTimeout(recChannel, milliseconds);
	}
	
	public void shutdown() {
		
		for(Future<?> f : futures) {
			f.cancel(true);
		}
		
		executor.shutdownNow();		
		
		//Unsubscribe from everything
		for(String channel : map.keySet()) {
			pubsubInterface.unsubscribe(channel);
		}
		
		//Shutdown pubsub server/client/whatever
		pubsubInterface.shutdown();
	}

	public static class ParserBuilder<ReceiveType, PublishType> { 
		
		//TODO: Document these fields to ensure understandability
		
		private final ArrayList<String> channels = new ArrayList<String>();
		private Metrics metrics;
		
		private final LinkedBlockingQueue<MessageTuple<PublishType>> sender = new LinkedBlockingQueue<MessageTuple<PublishType>>();	
		private final HashMap<String, Receiver<ReceiveType>> map = new HashMap<String, Receiver<ReceiveType>>();	
		private final ExecutorService executor; //3 threads, just to make sure
		private final Vector<Future<?>> futures;	
		private CountDownLatch latch = new CountDownLatch(2); 
		private final IPubSubbable<ReceiveType, PublishType> pubsubInterface;	
		private boolean running = true;
		
		public ParserBuilder(IPubSubbable<ReceiveType, PublishType> pubsubInterface) {
			
			this.pubsubInterface = pubsubInterface;
			
			//Create a thread factory and name the threads, so we can tell what's going on with the profiler
			ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("PARSER-THREAD-%d").build();
			
			futures = new Vector<Future<?>>();
			executor = Executors.newFixedThreadPool(8, namedThreadFactory);					
		}
		
		public ParserBuilder<ReceiveType, PublishType> withSubscriptionTo(String channel) {
			channels.add(channel);
			return this;
		}
		
		///Could be designed in a better way
		public ParserBuilder<ReceiveType, PublishType> withMetrics() {
			metrics = new Metrics();
			return this;
		}
		
		public Parser<ReceiveType, PublishType> build() {
			
			//Initialize queues for all the message types
			for (String t : channels) { 
				this.map.put(t, new Receiver<ReceiveType>());
			}
	
			//Switch tasks based on what option has been selected
			
			Callable<Void> messageReceiver; 
			Callable<Void> messageSender;
	
			if(metrics == null) {
				messageReceiver = this.messageReceiverNone;
				messageSender = this.messageSenderNone;
			} else {
				messageReceiver = this.messageReceiverMetrics;
				messageSender = this.messageSenderMetrics;
			}

			//Add all of our tasks to the executor
			futures.add(executor.submit(messageReceiver));
			futures.add(executor.submit(messageSender));
			
			//Subscribing to channels after all the setup has been completed
			for (String channel : channels) {
				//System.out.println("Subscribing to: " + channel);
				pubsubInterface.subscribe(channel);
			}			
			
			//Wait for all the threads to start
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return new Parser<ReceiveType, PublishType>(this);
		}
		
		/**
		 * Callables implemented for optimization purposes.  I should really find a better way to accomplish this goal...
		 */
		
		private final Callable<Void> messageReceiverMetrics = new Callable<Void>() {
			
			@Override
			public Void call() {
			
				latch.countDown();
									
				//Blocking?
				while(running) {
					
					MessageTuple<ReceiveType> m = pubsubInterface.getMessage();
					
					//Change this method
					if(metrics != null) {
						metrics.incrementReceived();
					}
					
					LinkedBlockingQueue<MessageTuple<ReceiveType>> lbq = map.get(m.channel).receiver;
					boolean result = lbq.offer(m);
					
					//Ensure we only store one message at a time.  Turns out this has little performance impact
					if(!result) {
						try {
							lbq.take();
						} catch (InterruptedException e) {
							running = false;
							e.printStackTrace();
						} 
						try {
							lbq.put(m);
						} catch (InterruptedException e) {
							running = false;
							e.printStackTrace();
						}
					}		
				}
				return null;
			}		
		};
		
		private final Callable<Void> messageReceiverNone = new Callable<Void>() {
			
			@Override
			public Void call() {
			
				latch.countDown();
									
				//Blocking?
				while(running) {
					
					MessageTuple<ReceiveType> m = pubsubInterface.getMessage();
					
					LinkedBlockingQueue<MessageTuple<ReceiveType>> lbq = map.get(m.channel).receiver;
					boolean result = lbq.offer(m);
					
					//Ensure we only store one message at a time.  Turns out this has little performance impact
					if(!result) {
						try {
							lbq.take();
						} catch (InterruptedException e) {
							running = false;
							e.printStackTrace();
						} 
						try {
							lbq.put(m);
						} catch (InterruptedException e) {
							running = false;
							e.printStackTrace();
						}
					}		
				}
				return null;
			}		
		};
		
		private final Callable<Void> messageSenderMetrics = new Callable<Void>() {

			@Override
			public Void call() {
				
				latch.countDown();
				
				while(running) {
					
					//Change this method
					if(metrics != null) {
						metrics.incrementSent();
					}
					
					MessageTuple<PublishType> m;
					try {
						m = sender.take();
						pubsubInterface.publish(m);
					} catch (InterruptedException e) {
						running = false;
						e.printStackTrace();
					}
				}
				return null;
			}		
		};
		
		private final Callable<Void> messageSenderNone = new Callable<Void>() {

			@Override
			public Void call() {
				
				latch.countDown();
				
				while(running) {
					
					MessageTuple<PublishType> m;
					try {
						System.out.println(sender.size());
						m = sender.take();
						pubsubInterface.publish(m);
					} catch (InterruptedException e) {
						running = false;
						e.printStackTrace();
					}
				}
				return null;
			}		
		};
				
	}
}
