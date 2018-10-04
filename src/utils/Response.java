package utils;

public class Response {

    private final int status;
    private final String body;
    private final String type;

    /**
     * @param type The type of the link the response is for (STREAM or DATA).
     * @param status The status of the response.
     * @param body Contains returned data for the request.
     */
    public Response(String type, int status, String body) {

        this.type = type;
        this.status = status;
        this.body = body;
    }

    /**
     * Returns the status of the response.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the body of the response.
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the type of the response.
     */
    public String getType() {
        return type;
    }
}
