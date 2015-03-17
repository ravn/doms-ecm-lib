package dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/15/13 Time: 2:33 PM To change this template use File | Settings | File
 * Templates.
 */
public class LinkPattern {

    private String name, description, value;

    public LinkPattern(String name,
                       String description,
                       String value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }
}
