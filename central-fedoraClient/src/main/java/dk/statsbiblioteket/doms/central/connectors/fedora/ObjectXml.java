package dk.statsbiblioteket.doms.central.connectors.fedora;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.util.xml.XSLT;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class handles the translation between raw foxml and the cleaned version we want to emit.
 */
public class ObjectXml {

    private String pid;
    private final String rawXml;
    private final FedoraRest fedora;
    private final Long asOfTime;

    /**
     * Construct the ObjectXml from a raw dump, with a fedora client
     * @param pid the pid of the object
     * @param rawXml the raw dump
     * @param fedora the fedora client
     * @param asOfTime the cut of date
     */
    public ObjectXml(String pid, String rawXml, FedoraRest fedora, Long asOfTime){
        this.pid = pid;

        this.rawXml = rawXml;
        this.fedora = fedora;
        this.asOfTime = asOfTime;
    }

    /**
     * Get the cleaned version of the xml
     * @return the cleaned version
     * @throws TransformerException
     * @throws BackendInvalidResourceException
     * @throws BackendMethodFailedException
     * @throws BackendInvalidCredsException
     */
    public String getCleaned() throws
                               TransformerException,
                               BackendInvalidResourceException,
                               BackendMethodFailedException,
                               BackendInvalidCredsException {
        String xml = modifyForDate(rawXml, asOfTime);
        xml = stripHiddenDatastreams(xml);
        xml = stripOldVersions(xml);
        xml = stubManagedDatastreams(xml);

        List<String> datastreamIDs = getManagedXmlDatastreams(xml);

        StringBuilder mutableXmlBlob = new StringBuilder(xml);

        for (String datastreamID : datastreamIDs) {
            mutableXmlBlob = inlineDatastream(mutableXmlBlob, datastreamID, pid, asOfTime);
        }


        return mutableXmlBlob.toString();


    }

    /**
     * Replace managed datastreams with a placeholderr
     * @param xml the xml
     * @return the xml with placeholders
     * @throws TransformerException
     */
    private String stubManagedDatastreams(String xml) throws TransformerException {
        Transformer transformer = XSLT.getLocalTransformer(
                Thread.currentThread().getContextClassLoader().getResource("xslt/placeHoldersForManagedDatastreams.xslt"));
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
        return result.toString();
    }


    /**
     * Strip the hidden datastreams like AUDIT
     * @param xml the xml string prestrip
     * @return the xml blob without a trace of the hidden datastreams
     * @throws TransformerException
     */
    protected String stripHiddenDatastreams(String xml) throws TransformerException {
        Transformer transformer = XSLT.getLocalTransformer(
                Thread.currentThread().getContextClassLoader().getResource("xslt/stripHiddenDatastreams.xslt"));
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
        return result.toString();
    }


    /**
     * Inline the given managed datastream. Will fetch the contents and insert it instead of the reference to the content.
     * @param xml the xml blob
     * @param dsid the datastream id
     * @param pid the pid of the object
     * @param asOfTime the cutoff date
     * @return A stringbuilder with the datastream inlined
     * @throws BackendInvalidResourceException
     * @throws BackendInvalidCredsException
     * @throws BackendMethodFailedException
     */
    protected StringBuilder inlineDatastream(StringBuilder xml, String dsid, String pid, Long asOfTime) throws
                                                                                                        BackendInvalidResourceException,
                                                                                                        BackendInvalidCredsException,
                                                                                                        BackendMethodFailedException {


        String realContent = fedora.getXMLDatastreamContents(pid, dsid, asOfTime);
        String cleanedContent = realContent.replaceFirst("\\A\\<\\?xml(.+?)\\?\\>", "").trim();

        String str = "PLACEHOLDER_PREFIX" + dsid + "PLACEHOLDER_POSTFIX";
        int placeholderIndex = xml.indexOf(str);
        return xml.replace(placeholderIndex, placeholderIndex + str.length(), cleanedContent);
    }

    /**
     * Get the list of DSIDs for managed datastreams with mimetype text/xml
     * @param xml the xml blob
     * @return the list of datastream IDs to be inlined
     * @throws TransformerException
     */
    protected List<String> getManagedXmlDatastreams(String xml) throws TransformerException {
        Transformer transformer = XSLT.getLocalTransformer(
                Thread.currentThread().getContextClassLoader().getResource("xslt/getManagedDatastreams.xslt"));
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
        String[] lines = result.toString().split("\n");
        ArrayList<String> realResult = new ArrayList<String>();
        for (String line : lines) {
            if (!line.isEmpty()) {
                realResult.add(line);
            }
        }
        return realResult;
    }

    /**
     * Strip all but the newest version of versionable datastreams
     * @param xml the xml blob
     * @return the xml blob with only one version of each datastream
     * @throws TransformerException
     */
    protected String stripOldVersions(String xml) throws TransformerException {
        Transformer transformer = XSLT.getLocalTransformer(
                Thread.currentThread().getContextClassLoader().getResource("xslt/stripOldVersions.xslt"));
        StringWriter result = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
        return result.toString();
    }

    /**
     * Strip all version of a datastream that are newer than the asOfTime. If asOfTime is null, no change
     * @param xml the xml blob
     * @param asOfTime the cutoff date
     * @return the xml with any newer datastream versions removed
     * @throws TransformerException
     */
    protected String modifyForDate(String xml, Long asOfTime) throws TransformerException {
        if (asOfTime != null){
            Date date = new Date(asOfTime);
            String dateForXslt = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
            Transformer transformer = XSLT.getLocalTransformer(
                    Thread.currentThread().getContextClassLoader().getResource("xslt/removeNewerDatastreams.xslt"));
            StringWriter result = new StringWriter();
            transformer.setParameter("highestCreated",dateForXslt);
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(result));
            return result.toString();

        }

        return xml;
    }


}
