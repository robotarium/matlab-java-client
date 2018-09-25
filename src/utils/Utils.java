package utils;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Stream;

public class Utils {

    public static String createMessageId() {

        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < 20) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, 20);
    }

    public static String createRequestLink(String nodeName) {
        return nodeName + '/' + "requests";
    }

    public static String createResponseLink(String nodeName, String messageId) {
        return nodeName + '/' + "responses" + '/' + messageId;
    }

    public static String createJsonRequest(String link, String method) {

        String id = createMessageId();
        var r = new Request(id, method, link);
        return new Gson().toJson(r);
    }

    public static String createJsonResponse(String type, int status, String body) {

        var r = new Response(type, status, body);
        return new Gson().toJson(r);
    }

    private static boolean isSubsetOf(ArrayList<String> one, ArrayList<String> two) {

        if(one.size() > two.size()) {
            return false;
        }

        for(int i = 0; i < one.size(); i++) {
            if(one.get(i) != two.get(i)) {
                return false;
            }
        }

        return true;
    }

    public static HashMap<String, JsonObject> parseNodeDescriptor(JsonObject nodeDescriptor) {
        String endPoint = nodeDescriptor.get("end_point").getAsString();

        if(endPoint != null){
            ArrayList<String> path = new ArrayList<>();
            path.add(endPoint);

            return parseNodeDescriptorHelper(path, new HashMap<String, JsonObject>(), nodeDescriptor);
        }

        return null;
    }

    private static HashMap<String, JsonObject> parseNodeDescriptorHelper(ArrayList<String> path, HashMap<String, JsonObject> result, JsonObject body) {

        JsonObject links = body.getAsJsonObject("links");

        // base case
        if(links == null) {
            if(body.get("type") == null) {
                throw new IllegalArgumentException("Leaf must contain type");
            }

            // If we're at the end of the recursive trail, add this info to the resulting hashmap
            String combinedPath = path.stream().reduce((a, b) -> a + "/" + b).get();
            result.put(combinedPath, body);

            return result;
        }

        var toVisit = links.entrySet();

        for (var x: toVisit) {
            String link = x.getKey();

            ArrayList<String> newPath = null;
            ArrayList<String> pathHere = new ArrayList<>(Arrays.asList(link.split("/")));

            // If the path started with '/', convert to absolute
            if(link.charAt(0) == '/') {
                // Path is relative. Append to existing path
                newPath = new ArrayList<>(path);
                newPath.addAll(pathHere.subList(1, pathHere.size()));
            } else {
                if(isSubsetOf(path, pathHere)) {
                    newPath = pathHere;
                }
            }

            parseNodeDescriptorHelper(newPath, result, x.getValue().getAsJsonObject());
        }

        return result;
    }

    public static ArrayList<JsonObject> parseNodeDescriptorRequests(JsonObject nodeDescriptor) {

        JsonArray requestsJson = nodeDescriptor.getAsJsonArray("requests");
        ArrayList<JsonObject> requests = new ArrayList<>();

        if(requestsJson == null) {
            return requests;
        }

        for (var x : requestsJson) {
           var request = x.getAsJsonObject();

           if(request.get("type") == null) {
               throw new IllegalArgumentException("Must have type fields specified in request");
           }

           if(request.get("link") == null) {
               throw new IllegalArgumentException("Must have link field specified in request");
           }

           if(request.get("required") == null) {
               request.add("required", new JsonParser().parse("true"));
           }

           // Request now has fields type, link, and required
            requests.add(request);
        }

        return requests;
    }

    public static void main(String[] args) {

        try {
            var f = new FileReader(args[0]);
            var nodeDescriptor = new Gson().fromJson(f, JsonObject.class);

            var result = parseNodeDescriptor(nodeDescriptor);
            var result2 = parseNodeDescriptorRequests(nodeDescriptor);

            System.out.println(result);
            System.out.println(result2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}