package utils;

import com.google.gson.Gson;

public class Request {

    String id;
    String method;
    String link;

    public Request(String id, String method, String link) {
        this.id = id;
        this.method = method;
        this.link = link;
    }
}


