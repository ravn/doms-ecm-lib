package dk.statsbiblioteket.doms.central.connectors.fedora.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A representation of a ECM View.
 */
public class View {
    /**
     * A list of names of RDF properties to follow to define the view.
     */
    private List<String> properties;

    /**
     * A list of names of RDF properties into this view defining the view.
     */
    private List<String> inverseProperties;

    /**
     * Whether this is a main object in the view.
     */
    private boolean main;

    /**
     * Initialises the view, with two empty datastreams.
     */
    public View() {
        this.properties = new ArrayList<String>();
        this.inverseProperties = new ArrayList<String>();
    }

    /**
     * Get the list of names of properties to follow to define the view.
     *
     * @return A list of names of properties to follow to define the view.
     */
    public List<String> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    /**
     * Set the list of names of properties to follow to define the view.
     *
     * @param properties A list of names of properties to follow to define the view.
     */
    public void setProperties(List<String> properties) {
        this.properties = properties;
    }


    public boolean addProperty(String s) {
        return properties.add(s);
    }

    public boolean addInverseProperty(String s) {
        return inverseProperties.add(s);
    }

    /**
     * Get the list of names of properties into this view defining the view.
     *
     * @return A list of names of properties into this view defining the view.
     */
    public List<String> getInverseProperties() {
        return Collections.unmodifiableList(inverseProperties);
    }

    /**
     * Set the list of names of properties into this view defining the view.
     *
     * @param inverseProperties A list of names of properties into this view defining the view.
     */
    public void setInverseProperties(List<String> inverseProperties) {
        this.inverseProperties = inverseProperties;
    }

    /**
     * Get whether this is the main object in this view.
     *
     * @return Whether this is the main object in this view.
     */
    public boolean isMain() {
        return main;
    }

    /**
     * Set whether this is the main object in this view.
     *
     * @param main Whether this is the main object in this view.
     */
    public void setMain(boolean main) {
        this.main = main;
    }
}
