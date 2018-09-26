package utils;

public class LinkDescriptor {
    private String body;
    private final String type;

    public LinkDescriptor(String body, String type) {
       this.body = body;
       this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }
}
