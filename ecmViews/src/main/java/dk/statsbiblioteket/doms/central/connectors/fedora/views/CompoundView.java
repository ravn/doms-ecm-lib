package dk.statsbiblioteket.doms.central.connectors.fedora.views;


import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.XpathUtils;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class describing a compound view, that is the result of combining information in all content models for an object.
 */
public class CompoundView {
    /**
     * A list of content model pids used to generate the compound view , in the order they were resolved.
     */
    List<String> pids;
    private static final Log LOG = LogFactory.getLog(CompoundView.class);

    /**
     * The view defined by the model.
     */
    Map<String, View> view;


    private static
    TimeSensitiveCache<String, CompoundView>
            longTermStorage =
            new TimeSensitiveCache<String, CompoundView>(1000 * 60 * 30, true, 20);
//TODO

    private static final String VIEWS_VIEWANGLE = "/view:views/view:viewangle";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VIEWS_VIEWANGLE_NAME = VIEWS_VIEWANGLE + "[@name='";
    private static final String VIEW_RELATIONS = "']/view:relations/*";
    private static final String VIEW_INVERSE_RELATIONS = "']/view:inverseRelations/*";
    ///view:views/view:viewangle"

    /**
     * Initialise a content model.
     *
     * @throws RuntimeException on trouble generating document.
     */
    public CompoundView() {
        pids = new ArrayList<String>();
        view = new HashMap<String, View>();
    }

    /**
     * Get the list of content model pids used to generate the compound view, in the order they were resolved.
     *
     * @return A list of pids.
     */
    public List<String> getPids() {
        return Collections.unmodifiableList(pids);
    }


    /**
     * Get the view defined by the model.
     *
     * @return The view defined by the model.
     */
    public Map<String, View> getView() {
        return view;
    }

    /**
     * Get the compound view for an object. This method will generate an abstract representation of the Compund view for
     * an object.
     *
     * @param pid      The pid for an object.
     * @param asOfTime
     *
     * @return The compound content model for the object or content model.
     */
    public static CompoundView getView(String pid,
                                       Fedora fedora,
                                       Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {


        LOG.trace("Entering getView with string '" + pid + "'");


        // Get starting point
        // TODO: Consider sorting this list, with children before parents?
/*

        if (!fedoraConnector.isDataObject(pid)){
            throw new FedoraIllegalContentException("The object '"+ pid +"' is " +
                                                    "not a data object");
        }
*/

        // Initialise list of base content models
        ObjectProfile profile = fedora.getObjectProfile(pid, asOfTime);
        List<String> models = profile.getContentModels();


        //Reduce the list to unique content models
        Set<String> pids1 = new TreeSet<String>();
        pids1.addAll(models);

        ArrayList<String> pids = new ArrayList<String>(pids1);
        CompoundView model = longTermStorage.get(pids.toString());
        if (model != null) {
            return model;
        }

        model = new CompoundView();

        // Update content model with info from all models.
        for (String p : pids) {
            LOG.trace("Getting view from object " + p);


            Document viewDoc;
            try {
                String viewXml;
                viewXml = fedora.getXMLDatastreamContents(p, Constants.VIEW_DATASTREAM, asOfTime);

                viewDoc = DOM.stringToDOM(viewXml, true);
            } catch (BackendInvalidResourceException e) {
                //TODO logging
                LOG.warn("Object '"
                         + p
                         + "' is declared as a content model for object '"
                         + pid
                         + "' but does not exist", e);
                LOG.warn("No VIEW datastream in content model '" + p + "'");
                continue;
            }
            LOG.trace("Entering updateView for content model " + p);
            updateView(model.getView(), viewDoc);

            // Check if this is the content model for a main object in some view
            setMainView(p, model.getView(), fedora);
        }
        longTermStorage.put(pids.toString(), model);

        LOG.trace("Got all views, returning");
        model.pids = pids;
        return model;
    }


    /**
     * Update the view with information from parsed viewdatastream. This includes properties and inverseProperties which
     * should be followed to generate the view. It is all added under the view with the given name.
     *
     * @param views   The map of views to update.
     * @param viewXml The datastream with information to add.
     */
    private static void updateView(Map<String, View> views,
                                   Document viewXml)
            throws
            BackendMethodFailedException {
        NodeList xpathResult;
        // Get all views.
        LOG.trace("Entering updateview with params");

        try {
            xpathResult = XpathUtils.xpathQuery(viewXml, VIEWS_VIEWANGLE);


        } catch (XPathExpressionException e) {
            throw new Error("XPath expression did not evaluate", e);
        }
        LOG.debug("Found " + xpathResult.getLength() + " view angles");

        for (int v = 0; v < xpathResult.getLength(); v++) {
            Node viewAngle = xpathResult.item(v);

            if (viewAngle.getNodeType() != Document.ELEMENT_NODE) {
                LOG.warn("View node is not a xml element");
                continue;
            }

            // Get the name
            Element e = (Element) viewAngle;
            String name = e.getAttribute(NAME_ATTRIBUTE);
            if (name != null && !name.equals("")) {

                //TODO Really restrict them to normal A-Z here.
                // Views may not have names containing '
                if (name.contains("'")) {
                    throw new BackendMethodFailedException("Views may not have names containing ',"
                                                           + " but view name was \""
                                                           + name
                                                           + "\"");
                }

                // Get or generate view for that name.
                View view = views.get(name);
                if (view == null) {
                    view = new View();
                    views.put(name, view);
                }
                // Update information for that view
                addViewRelations(view, viewXml, name);
                addViewInverseRelations(view, viewXml, name);
            } else {
                LOG.warn("Could not read the name of the view");
            }
        }
    }

    /**
     * Update list of properties for this view.
     *
     * @param view    The view to update.
     * @param viewXml The document to get the information from.
     * @param name    The name to update the view for.
     */
    private static void addViewRelations(View view,
                                         Document viewXml,
                                         String name)
            throws
            BackendMethodFailedException {
        NodeList xpathResult;
        try {
            // FIXME: Names with ' will throw errors
            xpathResult = XpathUtils.xpathQuery(viewXml, VIEWS_VIEWANGLE_NAME + name + VIEW_RELATIONS);
        } catch (XPathExpressionException e) {
            throw new BackendMethodFailedException("XPath expression failed", e);
        }
        for (int l = 0; l < xpathResult.getLength(); l++) {
            Node n = xpathResult.item(l);
            if (n.getNodeType() == Document.ELEMENT_NODE) {
                Element e = (Element) n;
                String viewelement = e.getNamespaceURI() + e.getLocalName();
                if (!view.getProperties().contains(viewelement)) {
                    view.addProperty(viewelement);
                }
            }
        }
    }

    /**
     * Update list of inverseProperties in this view.
     *
     * @param view    The view to update.
     * @param viewXml The document to get the information from.
     * @param name    The name to update the view for.
     */
    private static void addViewInverseRelations(View view,
                                                Document viewXml,
                                                String name) {
        NodeList xpathResult;
        try {
            // FIXME: Names with ' will throw errors
            xpathResult = XpathUtils.xpathQuery(viewXml, VIEWS_VIEWANGLE_NAME + name + VIEW_INVERSE_RELATIONS);
        } catch (XPathExpressionException e) {
            throw new Error("XPath expression did not evaluate", e);
        }
        for (int l = 0; l < xpathResult.getLength(); l++) {
            Node n = xpathResult.item(l);
            if (n.getNodeType() == Document.ELEMENT_NODE) {
                Element e = (Element) n;
                String viewelement = e.getNamespaceURI() + e.getLocalName();
                if (!view.getInverseProperties().contains(viewelement)) {
                    view.addInverseProperty(viewelement);
                }
            }
        }
    }

    /**
     * Set whether this is a main view, by parsing the relations defining this.
     *
     * @param pid             The pid to parse relations for
     * @param views           The views in the content model to update.
     * @param fedoraConnector
     */
    private static void setMainView(String pid,
                                    Map<String, View> views,
                                    Fedora fedoraConnector)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {


        List<FedoraRelation> relations = fedoraConnector.getNamedRelations(pid, Constants.ENTRY_RELATION, null);


        for (FedoraRelation relation : relations) {
            String viewname = relation.getObject();
            View view = views.get(viewname);
            if (view == null) {
                view = new View();
                views.put(viewname, view);
            }
            view.setMain(true);
        }

    }

}
