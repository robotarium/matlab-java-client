package utils;

public class LinkRequestDescriptor {

    private final String link;
    private final String type;
    private final boolean required;

    /**
     * @param link The link for which the request is made.
     * @param type The type of the requested link (STREAM or DATA).
     * @param required Whether the link is required or not (optional).
     */
    public LinkRequestDescriptor(String link, String type, boolean required) {
        this.link = link;
        this.type = type;
        this.required = required;
    }

    /**
     * Returns whether the link is required.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the type of the link.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the link.
     */
    public String getLink() {
        return link;
    }
}
