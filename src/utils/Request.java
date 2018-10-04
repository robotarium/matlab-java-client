package utils;

import com.google.gson.Gson;

public class Request {

    private final String id;
    private final String method;
    private final String link;

    /**
     * @param id The ID for the request.  Should be something large and random.
     * @param method Method for the request (e.g., GET, PUT).
     * @param link Link for which the request is made.
     */
    public Request(String id, String method, String link) {
        this.id = id;
        this.method = method;
        this.link = link;
    }

    /**
     * Returns the ID of the request.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the method of the request.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the link of the request.
     */
    public String getLink() {
        return link;
    }
}


