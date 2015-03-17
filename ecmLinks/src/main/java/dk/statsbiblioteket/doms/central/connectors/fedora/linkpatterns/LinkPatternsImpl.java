package dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.util.EncodingType;
import dk.statsbiblioteket.doms.util.Parameter;
import dk.statsbiblioteket.doms.util.ReplaceTools;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/15/13 Time: 2:17 PM To change this template use File | Settings | File
 * Templates.
 */
public class LinkPatternsImpl implements LinkPatterns {


    Fedora fedora;
    private String fedoraLocation;
    private Unmarshaller jaxb;
    XPathSelector xpath = DOM.createXPathSelector("lp", "http://doms.statsbiblioteket.dk/types/linkpattern/0/1/#");

    public LinkPatternsImpl(Fedora fedora,
                            String fedoraLocation)
            throws
            JAXBException {
        this.fedora = fedora;
        this.fedoraLocation = fedoraLocation;
        jaxb =
                JAXBContext.newInstance("dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns.generated")
                           .createUnmarshaller();
    }

    @Override
    public List<LinkPattern> getLinkPatterns(String pid,
                                             Long asOfDate)
            throws
            BackendInvalidResourceException,
            BackendInvalidCredsException,
            BackendMethodFailedException {

        List<LinkPattern> linkPatterns = new ArrayList<LinkPattern>();

        ObjectProfile profile = fedora.getObjectProfile(pid, asOfDate);
        List<String> contentmodels = profile.getContentModels();
        for (String contentmodel : contentmodels) {
            try {
                String linkPatternStream = fedora.getXMLDatastreamContents(contentmodel, "LINK_PATTERN", asOfDate);
                Document doc = DOM.stringToDOM(linkPatternStream, true);
                NodeList linkPatternNodes = xpath.selectNodeList(doc, "/lp:linkPatterns/lp:linkPattern");
                for (int i = 0; i < linkPatternNodes.getLength(); i++) {
                    Node linkPatternNode = linkPatternNodes.item(i);
                    String name = xpath.selectString(linkPatternNode, "lp:name");
                    String alt_text = xpath.selectString(linkPatternNode, "lp:description");
                    String value = xpath.selectString(linkPatternNode, "lp:value");


                    Map<Parameter, List<String>> parameters = getParametersFromXpath(pid, asOfDate, linkPatternNode);

                    List<Parameter> declaredParameters = new ArrayList<Parameter>(parameters.keySet());

                    //Add in the default parameters
                    ReplaceTools.setDefaultParameters(declaredParameters, parameters, profile, fedora, fedoraLocation);
                    ReplaceTools.setContextParameters(declaredParameters, parameters);
                    value = ReplaceTools.fillInParameters(parameters, value, declaredParameters, EncodingType.URL);

                    LinkPattern linkPattern = new LinkPattern(name, alt_text, value);
                    linkPatterns.add(linkPattern);
                }
            } catch (BackendInvalidResourceException e) {
                continue;
            }
        }
        return linkPatterns;


    }


    private Map<Parameter, List<String>> getParametersFromXpath(String pid,
                                                                Long asOfDate,
                                                                Node node)
            throws
            BackendInvalidResourceException,
            BackendInvalidCredsException,
            BackendMethodFailedException {

        Map<Parameter, List<String>> result = new HashMap<Parameter, List<String>>();

        NodeList replacements = xpath.selectNodeList(node, "lp:replacements/lp:replacement");
        for (int i = 0; i < replacements.getLength(); i++) {
            Node replacementNode = replacements.item(i);


            String key = null;
            String datastream = null;
            String xpathValue = null;

            key = xpath.selectString(replacementNode, "lp:key");
            datastream = xpath.selectString(replacementNode, "lp:datastream");
            xpathValue = xpath.selectString(replacementNode, "lp:xpath");
            Boolean repeatable = xpath.selectBoolean(replacementNode, "lp:repeatable", false);
            String prefix = xpath.selectString(replacementNode, "lp:prefix", "");

            if (key == null || datastream == null || xpathValue == null) {
                continue;
            }


            String datastreamContents = fedora.getXMLDatastreamContents(pid, datastream, asOfDate);
            Document doc = DOM.stringToDOM(datastreamContents);
            XPathSelector xpathSelector = DOM.createXPathSelector();

            Parameter parameter = new Parameter(key, prefix, true, repeatable, "", "", "", true);

            if (repeatable) {
                NodeList values = xpathSelector.selectNodeList(doc, xpathValue);
                for (int j = 0; j < values.getLength(); j++) {
                    Node value;
                    value = values.item(j);
                    List<String> foundValues = result.get(parameter);
                    if (foundValues == null) {
                        foundValues = Arrays.asList(value.getTextContent());
                    } else {
                        foundValues.add(value.getTextContent());
                    }
                    result.put(parameter, foundValues);
                }

            } else {
                String value = xpathSelector.selectString(doc, xpathValue);
                List<String> values = result.get(parameter);
                if (values == null) {
                    values = Arrays.asList(value);
                } else {
                    values.add(value);
                }
                result.put(parameter, values);


            }
        }
        return result;
    }


}
