package utils;

public class Response {

    private final int status;
    private final String body;
    private final String type;

    public Response(String type, int status, String body) {

        this.type = type;
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public String getType() {
        return type;
    }
}
