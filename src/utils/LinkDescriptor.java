package utils;

public class LinkDescriptor {
    private final String body;
    private final String type;

    /**
     * Creates a link descriptor.
     *
     * @param body Represents the body of the descriptor, containing relevant data.
     * @param type Repreents the type of the descriptor.  Must be STREAM or DATA
     */
    public LinkDescriptor(String body, String type) {
       this.body = body;
       this.type = type;
    }

    /**
     * Returns the body of the descriptor.
     */
    public String getBody() {
        return body;
    }

    /**
     *  Returns the type of the descriptor.
     */
    public String getType() {
        return type;
    }
}
