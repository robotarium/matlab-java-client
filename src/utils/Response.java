package utils;

public class Response {

    private int status;
    private String body;
    private String type;

    public Response(String type, int status, String body) {

        this.type = type;
        this.status = status;
        this.body = body;
    }
}
