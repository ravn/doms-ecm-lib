package dk.statsbiblioteket.doms.central.connectors.fedora.templates;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PidGenerator;
import dk.statsbiblioteket.doms.central.connectors.fedora.tripleStore.TripleStore;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.FedoraUtil;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.XpathUtils;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:46 PM To change this template use File | Settings | File
 * Templates.
 */
public class TemplatesImpl implements Templates {

    Fedora fedora;
    TripleStore ts;

    private static final Log LOG = LogFactory.getLog(TemplatesImpl.class);

    private static final String FOXML_DIGITAL_OBJECT_PID = "/foxml:digitalObject/@PID";
    private static final
    String
            RELSEXT_ABOUT =
            "/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
            + "foxml:datastreamVersion[position()=last()]/"
            + "foxml:xmlContent/rdf:RDF/"
            + "rdf:Description/@rdf:about";
    private static final
    String
            DCIDENTIFIER =
            "/foxml:digitalObject/foxml:datastream[@ID='DC']/"
            + "foxml:datastreamVersion[position()=last()]/"
            + "foxml:xmlContent/oai_dc:dc/dc:identifier";
    private static final
    String
            OAIDC =
            "/foxml:digitalObject/foxml:datastream[@ID='DC']/"
            + "foxml:datastreamVersion[position()=last()]/"
            + "foxml:xmlContent/oai_dc:dc";
    private static final
    String
            ISTEMPLATEFOR =
            "/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
            + "foxml:datastreamVersion[position()=last()]/"
            + "foxml:xmlContent/rdf:RDF/"
            + "rdf:Description/doms:isTemplateFor";
    private static final String DATASTREAM_EVENTS = "/foxml:digitalObject/foxml:datastream[@ID='EVENTS']";
    private static final String DATASTREAM_AUDIT = "/foxml:digitalObject/foxml:datastream[@ID='AUDIT']";
    private static final String DATASTREAM_NODES = "/foxml:digitalObject/foxml:datastream";

    private static final String DATASTREAM_CREATED = "/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion";
    private static final
    String
            OBJECTPROPERTY_CREATED =
            "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#createdDate']";
    private static final
    String
            OBJECTPROPERTIES_LSTMODIFIED =
            "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/view#lastModifiedDate']";
    private PidGenerator pidGenerator;

    public TemplatesImpl(Fedora fedora, PidGenerator pidGenerator, TripleStore ts) {
        this.fedora = fedora;
        this.pidGenerator = pidGenerator;
        this.ts = ts;
    }


    @Override
    public String cloneTemplate(String templatepid,
                                List<String> oldIDs,
                                String logMessage)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            ObjectIsWrongTypeException,
            BackendInvalidResourceException,
            PIDGeneratorException {
        //working
        templatepid = FedoraUtil.ensurePID(templatepid);
        LOG.trace("Entering cloneTemplate with param '" + templatepid + "'");


        if (!fedora.isTemplate(templatepid, null)) {
            throw new ObjectIsWrongTypeException("The pid (" + templatepid +
                                                 ") is not a pid of a template");
        }

        // Get the document
        String contents = fedora.getObjectXml(templatepid, null);
        Document document = DOM.stringToDOM(contents, true);

        //document.normalize();

        String newPid = pidGenerator.generateNextAvailablePID("clone_");
        LOG.debug("Generated new pid '" + newPid + "'");

        try {
            removeEvents(document);
            LOG.trace("Events removed");
            removeAudit(document);
            LOG.trace("Audit removed");
            removeDatastreamVersions(document);
            LOG.trace("Datastreamsversions removed");

            // Replace PID
            replacePid(document, templatepid, newPid);
            LOG.trace("Pids replaced");


            removeDCidentifier(document);
            LOG.trace("DC identifier removed");

            addOldIdentifiers(document, oldIDs);
            LOG.trace("Added old identifiers");

            removeXSI_DC(document);
            LOG.trace("XSI stuff removed from DC");


            removeCreated(document);
            LOG.trace("CREATED removed");

            removeLastModified(document);
            LOG.trace("Last Modified removed");

            removeTemplateRelation(document);
            LOG.trace("Template relation removed");
        } catch (XPathExpressionException e) {
            throw new BackendMethodFailedException("Template object '"
                                                   + templatepid
                                                   + "' did not contain the correct structure", e);
        }

        //reingest the object
        return fedora.ingestDocument(document, logMessage + "; " + "Cloned from template '" + templatepid);

    }

    /**
     * Removes the EVENTS datastream
     *
     * @param document the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeEvents(Document document) throws XPathExpressionException {
        removeExpathList(document, DATASTREAM_EVENTS);
    }


    private void addOldIdentifiers(Document document,
                                   List<String> oldIDs)
            throws
            XPathExpressionException {

        if (oldIDs != null && !oldIDs.isEmpty()) {

            Node dcNode = XpathUtils.xpathQuerySingle(document, OAIDC);
            if (dcNode != null) {
                String namespace = dcNode.getNamespaceURI();
                String prefix = dcNode.getPrefix();

                for (String oldID : oldIDs) {
                    Element element = document.createElementNS(namespace, prefix + ":identifier");
                    element.setTextContent(oldID);
                    dcNode.appendChild(element);
                }
            }
        }
    }

    private void removeXSI_DC(Document document)
            throws
            XPathExpressionException {
/*        removeExpathList(document, XSI_TAGS1);*/
        removeAttribute(document, OAIDC, "xsi:schemaLocation");
    }

    /**
     * Private helper method for cloneTemplate. In a document, replaces the mention of oldpid with newpid
     *
     * @param doc    the document to work on
     * @param oldpid the old pid
     * @param newpid the new pid
     *
     * @throws javax.xml.xpath.XPathExpressionException
     *          if there was
     */
    private void replacePid(Document doc,
                            String oldpid,
                            String newpid)
            throws
            XPathExpressionException {

        LOG.trace("Entering replacepid");
        replateAttribute(doc, FOXML_DIGITAL_OBJECT_PID, FedoraUtil.ensurePID(newpid));


        replateAttribute(doc, RELSEXT_ABOUT, FedoraUtil.ensureURI(newpid));


    }

    /**
     * Utility method for removing all nodes from a query. Does not work for attributes
     *
     * @param doc   the object
     * @param query the adress of the nodes
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeExpathList(Document doc,
                                  String query)
            throws
            XPathExpressionException {
        NodeList nodes = XpathUtils.
                                           xpathQuery(doc, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                node.getParentNode().removeChild(node);

            }
        }
    }

    /**
     * Utility method for changing the value of an attribute
     *
     * @param doc   the object
     * @param query the location of the Attribute
     * @param value the new value
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void replateAttribute(Document doc,
                                  String query,
                                  String value)
            throws
            XPathExpressionException {
        NodeList nodes = XpathUtils.
                                           xpathQuery(doc, query);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            node.setNodeValue(value);
        }

    }

    /**
     * Utility method for removing an attribute
     *
     * @param doc       the object
     * @param query     the adress of the node element
     * @param attribute the name of the attribute
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeAttribute(Document doc,
                                 String query,
                                 String attribute)
            throws
            XPathExpressionException {
        NodeList nodes;

        nodes = XpathUtils.xpathQuery(doc, query);

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            NamedNodeMap attrs = node.getAttributes();

            if (attrs.getNamedItem(attribute) != null) {
                attrs.removeNamedItem(attribute);
            }

        }
    }

    /**
     * Removes the DC identifier from the DC datastream
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeDCidentifier(Document doc)
            throws
            XPathExpressionException {
        //Then remove the pid in dc identifier
        removeExpathList(doc, DCIDENTIFIER);
    }


    /**
     * Removes all template relations
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeTemplateRelation(Document doc)
            throws
            XPathExpressionException {
        // Remove template relation

        //TODO Constant for template relation
        removeExpathList(doc, ISTEMPLATEFOR);


    }

    /**
     * Removes the AUDIT datastream
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeAudit(Document doc)
            throws
            XPathExpressionException {

        removeExpathList(doc, DATASTREAM_AUDIT);

    }

    /**
     * Removes all datastream versions, except the newest
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeDatastreamVersions(Document doc)
            throws
            XPathExpressionException {
        NodeList relationNodes;

        NodeList datastreamNodes = XpathUtils.xpathQuery(doc, DATASTREAM_NODES);

        for (int i = 0; i < datastreamNodes.getLength(); i++) {
            Node datastreamNode = datastreamNodes.item(i);
            Node newest = datastreamNode.getLastChild();
            while (newest != null && newest.getNodeType() != Node.ELEMENT_NODE) {
                newest = newest.getPreviousSibling();
            }
            while (datastreamNode.hasChildNodes()) {
                datastreamNode.removeChild(datastreamNode.getFirstChild());
            }
            datastreamNode.appendChild(newest);
        }
    }

    /**
     * Removes the CREATED attribute on datastreamVersion and the createdDate objectProperty
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeCreated(Document doc)
            throws
            XPathExpressionException {
        LOG.trace("Entering removeCreated");
        removeAttribute(doc, DATASTREAM_CREATED, "CREATED");

        removeExpathList(doc, OBJECTPROPERTY_CREATED);


    }

    /**
     * Removes the lastModifiedDate objectDate
     *
     * @param doc the object
     *
     * @throws XPathExpressionException if a xpath expression did not evaluate
     */
    private void removeLastModified(Document doc)
            throws
            XPathExpressionException {

        removeExpathList(doc, OBJECTPROPERTIES_LSTMODIFIED);

    }

}
