package utils;

public class LinkRequestDescriptor {

    private final String link;
    private final String type;
    private final boolean required;


    public LinkRequestDescriptor(String link, String type, boolean required) {
        this.link = link;
        this.type = type;
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public String getType() {
        return type;
    }

    public String getLink() {
        return link;
    }
}
