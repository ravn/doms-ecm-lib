/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.central.connectors.fedora;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.Connector;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.DatastreamProfileType;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.DatastreamType;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectDatastreams;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectFieldsType;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.ResultType;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.Validation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.DatastreamProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectType;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.SearchResult;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.DateUtils;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Implementation of the {@link Fedora} interface using REST to communicate with Fedora.
 */
public class FedoraRest extends Connector implements Fedora {

    private static final String AS_OF_DATE_TIME = "asOfDateTime";
    private static Log log = LogFactory.getLog(FedoraRest.class);
    private WebResource restApi;
    private String port;
    private final int maxTriesPut;
    private final int maxTriesPost;
    private final int maxTriesDelete;
    private final int retryDelay;

    /**
     * Initialise connector.
     *
     * @param creds    Credentials for communicating with Fedora.
     * @param location URL to Fedora
     * @throws MalformedURLException On illegal URLs in location parameter.
     */
    public FedoraRest(Credentials creds, String location) throws MalformedURLException {
        this(creds, location, 1, 1, 1, 100);
    }

    /**
     * Initialise connector where we retry a number of times on 409 results. This is used because URLConnection may
     * retry PUT or POST requests on timeout or connection errors, and this may result in spurious locks where the
     * original request still has the object locked.
     * We delay between retries, and the delay is done with exponential backoff, first waiting {@link #retryDelay}, and
     * 2*{@link #retryDelay}, then 4*{@link #retryDelay} and so forth.
     *
     * @param creds          Credentials for communicating with Fedora.
     * @param location       URL to Fedora
     * @param maxTriesPut    The number of tries to retry on 409 on PUT requests
     * @param maxTriesPost   The number of tries to retry on 409 on POST requests
     * @param maxTriesDelete The number of tries to retry on 409 on DELETE requests
     * @param retryDelay     The delay to wait between tries (with exponential backoff)
     * @throws MalformedURLException On illegal URLs in location parameter.
     */
    public FedoraRest(Credentials creds, String location, int maxTriesPut, int maxTriesPost, int maxTriesDelete,
                      int retryDelay) throws MalformedURLException {
        super(creds, location);
        this.maxTriesPut = maxTriesPut;
        this.maxTriesPost = maxTriesPost;
        this.maxTriesDelete = maxTriesDelete;
        this.retryDelay = retryDelay;

        restApi = client.resource(location + "/objects");
        restApi.addFilter(new HTTPBasicAuthFilter(creds.getUsername(), creds.getPassword()));
        port = calculateFedoraPort(location);
    }

    private String calculateFedoraPort(String location) {
        String portString = location.substring(location.lastIndexOf(':') + 1);
        portString = portString.substring(0, portString.indexOf('/'));
        return portString;
    }

    @Override
    public boolean exists(String pid, Long asOfDateTime)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            ObjectProfile profile = getLimitedObjectProfile(pid, asOfDateTime);
        } catch (BackendInvalidResourceException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isDataObject(String pid, Long asOfDateTime)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            ObjectProfile profile = getLimitedObjectProfile(pid, asOfDateTime);
            return profile.getType().equals(ObjectType.DATA_OBJECT);
        } catch (BackendInvalidResourceException e) {
            return false;
        }
    }

    @Override
    public boolean isTemplate(String pid, Long asOfDateTime)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            ObjectProfile profile = getObjectProfile(pid, asOfDateTime);
            return profile.getType().equals(ObjectType.TEMPLATE);
        } catch (BackendInvalidResourceException e) {
            return false;
        }

    }

    @Override
    public boolean isContentModel(String pid, Long asOfDateTime)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            ObjectProfile profile = getLimitedObjectProfile(pid, asOfDateTime);
            return profile.getType().equals(ObjectType.CONTENT_MODEL);
        } catch (BackendInvalidResourceException e) {
            return false;
        }

    }

    @Override
    public String getObjectXml(String pid, Long asOfTime)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {

        try {
            //Get basic fedora profile

            //Get the object xml

            //Strip the old versions

            //Search for managed datastreams with format text/xml

            //retrieve and insert the content

            String xml = getRawXml(pid);
            ObjectXml objectXml = new ObjectXml(pid, xml, this, asOfTime);

            return objectXml.getCleaned();
        } catch (UniformInterfaceException e) {
            handleResponseException(pid, 1, 1, e);
            throw e;
        } catch (TransformerException e) {
            //TODO Not really a backend exception
            throw new BackendMethodFailedException("Failed to transform object to output format", e);
        }
    }

    protected String getRawXml(String pid) {
        return restApi.path("/").path(urlEncode(pid)).path("/objectXML").type(MediaType.TEXT_XML_TYPE)
                .get(String.class);
    }

    private String StringOrNull(Long time) {
        if (time != null && time > 0) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter.format(new Date(time));
        }
        return "";
    }

    @Override
    public String ingestDocument(Document document, String logmessage)
            throws BackendMethodFailedException, BackendInvalidCredsException {
        String payload;
        try {
            payload = DOM.domToString(document);
        } catch (TransformerException e) {
            //TODO: This is not a backend exception
            throw new BackendMethodFailedException("Supplied document not valid", e);
        }
        WebResource.Builder request = restApi.path("/").path(urlEncode("new")).type(MediaType.TEXT_XML_TYPE);
        int tries = 0;
        while (true) {
            tries++;
            try {
                return request.post(String.class, payload);
            } catch (UniformInterfaceException e) {
                try {
                    handleResponseException("new", tries, maxTriesPost, e);
                } catch (BackendInvalidResourceException e1) {
                    //Ignore, never happens
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    @Override
    public ObjectProfile getLimitedObjectProfile(String pid, Long asOfTime) throws
                                                                             BackendInvalidResourceException,
                                                                             BackendMethodFailedException,
                                                                             BackendInvalidCredsException {
        try {
            //Get basic fedora profile
            dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectProfile profile;
            profile = restApi.path("/")
                             .path(urlEncode(pid))
                             .queryParam("format", "text/xml")
                             .get(dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectProfile.class);
            ObjectProfile prof = new ObjectProfile();
            prof.setObjectCreatedDate(profile.getObjCreateDate().toGregorianCalendar().getTime());
            prof.setObjectLastModifiedDate(profile.getObjLastModDate().toGregorianCalendar().getTime());
            prof.setLabel(profile.getObjLabel());
            prof.setOwnerID(profile.getObjOwnerId());
            prof.setState(profile.getObjState());
            prof.setPid(profile.getPid());
            List<String> contentmodels = new ArrayList<String>();
            for (String s : profile.getObjModels().getModel()) {
                if (s.startsWith("info:fedora/")) {
                    s = s.substring("info:fedora/".length());
                }
                contentmodels.add(s);
            }
            prof.setContentModels(contentmodels);

            //decode type
            prof.setType(ObjectType.DATA_OBJECT);
            if (prof.getContentModels().contains("fedora-system:ContentModel-3.0")) {
                prof.setType(ObjectType.CONTENT_MODEL);
            }
            if (prof.getContentModels().contains("doms:ContentModel_File")) {
                prof.setType(ObjectType.FILE);
            }
            if (prof.getContentModels().contains("doms:ContentModel_Collection")) {
                prof.setType(ObjectType.COLLECTION);
            }
            return prof;
        } catch (UniformInterfaceException e)  {
            handleResponseException(pid, 1, 1, e);
            throw e;
        }
    }

    @Override
    public ObjectProfile getObjectProfile(String pid, Long asOfTime)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        try {
            //Get basic fedora profile
            dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectProfile profile;
            profile = restApi.path("/").path(urlEncode(pid)).queryParam("format", "text/xml")
                    .get(dk.statsbiblioteket.doms.central.connectors.fedora.generated.ObjectProfile.class);
            ObjectProfile prof = new ObjectProfile();
            prof.setObjectCreatedDate(profile.getObjCreateDate().toGregorianCalendar().getTime());
            prof.setObjectLastModifiedDate(profile.getObjLastModDate().toGregorianCalendar().getTime());
            prof.setLabel(profile.getObjLabel());
            prof.setOwnerID(profile.getObjOwnerId());
            prof.setState(profile.getObjState());
            prof.setPid(profile.getPid());
            List<String> contentmodels = new ArrayList<String>();
            for (String s : profile.getObjModels().getModel()) {
                if (s.startsWith("info:fedora/")) {
                    s = s.substring("info:fedora/".length());
                }
                contentmodels.add(s);
            }
            prof.setContentModels(contentmodels);

            //Get relations
            List<FedoraRelation> relations = getNamedRelations(pid, null, asOfTime);
            prof.setRelations(relations);

            //get Datastream list
            ObjectDatastreams datastreams = restApi.path("/").path(urlEncode(pid)).path("/datastreams")
                    .queryParam("format", "text/xml").get(ObjectDatastreams.class);
            List<DatastreamProfile> pdatastreams = new ArrayList<DatastreamProfile>();
            for (DatastreamType datastreamType : datastreams.getDatastream()) {
                pdatastreams.add(getDatastreamProfile(pid, datastreamType.getDsid(), asOfTime));
            }
            prof.setDatastreams(pdatastreams);

            //decode type
            prof.setType(ObjectType.DATA_OBJECT);
            if (prof.getContentModels().contains("fedora-system:ContentModel-3.0")) {
                prof.setType(ObjectType.CONTENT_MODEL);
            }
            if (prof.getContentModels().contains("doms:ContentModel_File")) {
                prof.setType(ObjectType.FILE);
            }
            if (prof.getContentModels().contains("doms:ContentModel_Collection")) {
                prof.setType(ObjectType.COLLECTION);
            }

            for (FedoraRelation fedoraRelation : prof.getRelations()) {
                String predicate = fedoraRelation.getPredicate();
                if (Constants.TEMPLATE_REL.equals(predicate)) {
                    prof.setType(ObjectType.TEMPLATE);
                    break;
                }
            }

            return prof;


        } catch (UniformInterfaceException e) {
            handleResponseException(pid, 1, 1, e);
            throw e;
        }

    }

    public DatastreamProfile getDatastreamProfile(String pid, String dsid, Long asOfTime)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        try {

            DatastreamProfileType fdatastream = restApi.path("/").path(urlEncode(pid)).path("/datastreams/").path(dsid)
                    .queryParam(AS_OF_DATE_TIME, StringOrNull(asOfTime)).queryParam("format", "text/xml")
                    .get(DatastreamProfileType.class);
            DatastreamProfile profile = new DatastreamProfile();
            profile.setID(fdatastream.getDsID());
            profile.setLabel(fdatastream.getDsLabel());
            profile.setState(fdatastream.getDsState());

            profile.setChecksum(fdatastream.getDsChecksum());
            profile.setChecksumType(fdatastream.getDsChecksumType());

            profile.setCreated(fdatastream.getDsCreateDate().toGregorianCalendar().getTime().getTime());
            profile.setFormatURI(fdatastream.getDsFormatURI());
            profile.setMimeType(fdatastream.getDsMIME());

            String type = fdatastream.getDsControlGroup();
            if (type.equals("X") || type.equals("M")) {
                profile.setInternal(true);
            } else if (type.equals("E") || type.equals("R")) {
                profile.setInternal(false);
                profile.setUrl(fdatastream.getDsLocation());
            }
            return profile;
        } catch (UniformInterfaceException e) {
            handleResponseException(pid, 1, 1, e);
            throw e;
        }

    }

    @Override
    public void modifyObjectState(String pid, String state, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }
        WebResource request = restApi.path("/").path(urlEncode(pid)).queryParam("state", state)
                .queryParam("logMessage", comment);
        int tries = 0;
        while (true) {
            tries++;
            try {
                request.put();
                return;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPut, e);
            }
        }
    }

    @Override
    public Date modifyDatastreamByValue(String pid, String datastream, ChecksumType checksumType, String checksum,
                                        byte[] contents, List<String> alternativeIdentifiers, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        try {
            return updateExistingDatastreamByValue(pid, datastream, checksumType, checksum, contents,
                                                   alternativeIdentifiers, comment, null, null);
        } catch (BackendInvalidResourceException e) {
            //perhaps the datastream did not exist
            return createDatastreamByValue(pid, datastream, checksumType, checksum, contents, alternativeIdentifiers,
                                           null, comment);
        }
    }

    @Override
    public Date modifyDatastreamByValue(String pid, String datastream, ChecksumType checksumType, String checksum,
                                        byte[] contents, List<String> alternativeIdentifiers, String comment,
                                        Long lastModifiedDate)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException,
            ConcurrentModificationException {
        try {
            return updateExistingDatastreamByValue(pid, datastream, checksumType, checksum, contents,
                                                   alternativeIdentifiers, comment, lastModifiedDate, null);
        } catch (BackendInvalidResourceException e) {
            //perhaps the datastream did not exist
            return createDatastreamByValue(pid, datastream, checksumType, checksum, contents, alternativeIdentifiers,
                                           null, comment);
        }
    }

    @Override
    public Date modifyDatastreamByValue(String pid, String datastream, ChecksumType checksumType, String checksum,
                                        byte[] contents, List<String> alternativeIdentifiers, String mimeType,
                                        String comment, Long lastModifiedDate)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException,
            ConcurrentModificationException {
        try {
            return updateExistingDatastreamByValue(pid, datastream, checksumType, checksum, contents,
                                                   alternativeIdentifiers, comment, lastModifiedDate, mimeType);
        } catch (BackendInvalidResourceException e) {
            //perhaps the datastream did not exist
            return createDatastreamByValue(pid, datastream, checksumType, checksum, contents, alternativeIdentifiers,
                                           mimeType, comment);
        }
    }

    private Date createDatastreamByValue(String pid, String datastream, ChecksumType checksumType, String checksum,
                                         byte[] contents, List<String> alternativeIdentifiers, String mimeType,
                                         String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (mimeType == null) {
            mimeType = "text/xml";
        }
        WebResource resource = getModifyDatastreamWebResource(pid, datastream, checksumType, checksum,
                                                              alternativeIdentifiers, comment, null, mimeType).
                queryParam("controlGroup", "M");
        int tries = 0;
        while (true) {
            tries++;
            try {
                WebResource.Builder request = resource.entity(new ByteArrayInputStream(contents), mimeType);
                DatastreamProfileType profile = request.post(DatastreamProfileType.class);
                return profile.getDsCreateDate().toGregorianCalendar().getTime();
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPost, e);
            }
        }
    }

    private WebResource getModifyDatastreamWebResource(String pid, String datastream, ChecksumType checksumType,
                                                       String checksum, List<String> alternativeIdentifiers,
                                                       String comment, Long lastModifiedDate, String mimeType) {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }

        WebResource resource = restApi.path("/").path(urlEncode(pid)).path("/datastreams/").path(urlEncode(datastream))
                .queryParam("logMessage", comment);

        if (alternativeIdentifiers != null) {
            for (String alternativeIdentifier : alternativeIdentifiers) {
                resource = resource.queryParam("altIDs", alternativeIdentifier);
            }
        }
        if (checksumType != null) {
            resource = resource.queryParam("checksumType", checksumType.toString());

        }
        if (checksum != null) {
            resource = resource.queryParam("checksum", checksum);

        }
        if (lastModifiedDate != null) {
            resource = resource.queryParam("lastModifiedDate", StringOrNull(lastModifiedDate));
        }
        if (mimeType != null) {
            resource = resource.queryParam("mimeType", mimeType);
        } /*else {
            resource = resource.queryParam("mimeType", "text/xml");
        }*/
        return resource;
    }

    private Date updateExistingDatastreamByValue(String pid, String datastream, ChecksumType checksumType,
                                                 String checksum, byte[] contents, List<String> alternativeIdentifiers,
                                                 String comment, Long lastModifiedDate, String mimeType)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException,
            ConcurrentModificationException {

        int tries = 0;
        while (true) {
            tries++;
            try {
                WebResource resource = getModifyDatastreamWebResource(pid, datastream, checksumType, checksum,
                                                                      alternativeIdentifiers, comment, lastModifiedDate,
                                                                      mimeType);
                WebResource.Builder header = resource.header(HttpHeaders.CONTENT_TYPE, null);
                WebResource.Builder builder;
                if (mimeType != null) {
                    builder = header.entity(new ByteArrayInputStream(contents), mimeType);
                } else {
                    builder = header.entity(new ByteArrayInputStream(contents));
                }
                DatastreamProfileType profile = builder.put(DatastreamProfileType.class);
                return profile.getDsCreateDate().toGregorianCalendar().getTime();
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPut, e);
            }
        }
    }

    @Override
    public void deleteObject(String pid, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        WebResource request = restApi.path("/").path(urlEncode(pid)).queryParam("logMessage", comment);
        int tries = 0;
        while (true) {
            tries++;
            try {
                request.delete();
                break;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesDelete, e);
            }
        }
    }

    @Override
    public void deleteDatastream(String pid, String datastream, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        WebResource request = restApi.path("/").path(urlEncode(pid)).path("/datastreams/").path(urlEncode(datastream))
                .queryParam("logMessage", comment);
        int tries = 0;
        while (true) {
            tries++;
            try {
                request.delete();
                return;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesDelete, e);
            }
        }
    }

    @Override
    public String getXMLDatastreamContents(String pid, String datastream, Long asOfTime)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        try {

            return restApi.path("/").path(urlEncode(pid)).path("/datastreams/").path(urlEncode(datastream))
                    .path("/content").queryParam(AS_OF_DATE_TIME, StringOrNull(asOfTime)).get(String.class);
        } catch (UniformInterfaceException e) {
            handleResponseException(pid, 1, 1, e);
            throw e;
        }
    }

    @Override
    public void addRelation(String pid, String subject, String predicate, String object, boolean literal,
                            String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }  //TODO fedora should take this logmessage

        if (!literal) {
            if (!object.startsWith("info:fedora/")) {
                object = "info:fedora/" + object;
            }
        }

        predicate = getAbsoluteURIAsString(predicate);
        if (subject == null || subject.isEmpty()) {
            subject = "info:fedora/" + pid;
        }

        WebResource request = restApi.path("/").path(pid).path("/relationships/new").queryParam("subject", subject)
                .queryParam("predicate", predicate).queryParam("object", object).queryParam("isLiteral", "" + literal);
        int tries = 0;
        while (true) {
            tries++;
            try {
                request.post();
                break;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPost, e);
            }
        }

        if (predicate.equals("http://doms.statsbiblioteket.dk/relations/default/0/1/#hasLicense")) {
            //this is a license relation, update the policy datastream
            if (object.startsWith("info:fedora/")) {
                object = object.substring("info:fedora/".length());
            }
            addExternalDatastream(pid, "POLICY", "Policy datastream", "http://localhost:" + port +
                                          "/fedora/objects/" + object + "/datastreams/LICENSE/content", null,
                                  "application/rdf+xml", null, null, "Adding license datastream");
        }
    }

    @Override
    public void addRelations(String pid, String subject, String predicate, List<String> objects, boolean literal,
                             String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }
        if (literal) {//cant handle literal yet
            for (String object : objects) {
                addRelation(pid, subject, predicate, object, literal, comment);
            }
        }
        if (objects.size() == 1) {//more efficient if only adding one relation
            addRelation(pid, subject, predicate, objects.get(0), literal, comment);
        }

        XPathSelector xpath = DOM.createXPathSelector("rdf", Constants.NAMESPACE_RDF);

        String datastream;
        if (subject == null || subject.isEmpty() || subject.equals(pid) || subject.equals("info:fedora/" + pid)) {
            subject = "info:fedora/" + pid;
            datastream = "RELS-EXT";
        } else {
            datastream = "RELS-INT";
        }

        String rels = getXMLDatastreamContents(pid, datastream, null);
        Document relsDoc = DOM.stringToDOM(rels, true);

        Node rdfDescriptionNode = xpath.selectNode(relsDoc, "/rdf:RDF/rdf:Description[@rdf:about='" + subject + "']");

        predicate = getAbsoluteURIAsString(predicate);

        String[] splits = predicate.split("#");

        for (String object : objects) {
            if (!object.startsWith("info:fedora/")) {
                object = "info:fedora/" + object;
            }

            Element relationsShipElement = relsDoc.createElementNS(splits[0] + "#", splits[1]);
            relationsShipElement.setAttributeNS(Constants.NAMESPACE_RDF, "rdf:resource", object);
            rdfDescriptionNode.appendChild(relationsShipElement);
        }

        byte[] bytes;
        try {
            bytes = DOM.domToString(relsDoc).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //TODO Not really a backend exception
            throw new BackendMethodFailedException("Failed to transform RELS-EXT", e);
        } catch (TransformerException e) {
            //TODO Not really a backend exception
            throw new BackendMethodFailedException("Failed to transform RELS-EXT", e);
        }
        modifyDatastreamByValue(pid, datastream, ChecksumType.MD5, null, bytes, null, "application/rdf+xml", comment,
                                null);
    }

    @Override
    public List<FedoraRelation> getNamedRelations(String pid, String name, Long asOfTime)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        try {
            //TODO use asOfTime here, when fedora supports it

            String subject = pid;
            if (!subject.startsWith("info:fedora/")) {
                subject = "info:fedora/" + subject;
            }
            WebResource temp = restApi.path("/").path(pid).path("/relationships/").queryParam("subject", subject)
                    .queryParam("format", "n-triples");
            if (name != null) {
                temp = temp.queryParam("predicate", name);
            }
            String relationString = temp.get(String.class);

            String[] lines = relationString.split("\n");
            List<FedoraRelation> relations = new ArrayList<FedoraRelation>();
            for (String line : lines) {
                String[] elements = line.split(" ");
                if (elements.length > 2) {
                    FedoraRelation rel = new FedoraRelation(cleanInfo(elements[0]), clean(elements[1]),
                                                            cleanInfo(elements[2]));
                    if (elements[2].startsWith("<info:fedora/")) {
                        rel.setLiteral(false);
                    } else {
                        rel.setLiteral(true);
                    }
                    relations.add(rel);
                }
            }
            return relations;
        } catch (UniformInterfaceException e) {
            handleResponseException(pid, 1, 1, e);
            throw e;
        }
    }


    private String cleanInfo(String element) {
        element = clean(element);

        if (element.startsWith("info:fedora/")) {
            element = element.substring("info:fedora/".length());
        }

        return element;
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void deleteRelation(String pid, String subject, String predicate, String object, boolean literal,
                               String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        } //TODO, fedora should take this logmessage

        if (!literal) {
            if (!object.startsWith("info:fedora/")) {
                object = "info:fedora/" + object;
            }
        }
        predicate = getAbsoluteURIAsString(predicate);

        WebResource request = restApi.path("/").path(pid).path("/relationships/").queryParam("predicate", predicate)
                .queryParam("object", object).queryParam("isLiteral", "" + literal);
        int tries = 0;
        while (true) {
            tries++;
            try {
                request.delete();
                return;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesDelete, e);
            }
        }
    }

    @Override
    public void modifyObjectLabel(String pid, String name, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }

        int tries = 0;
        WebResource request = restApi.path("/").path(urlEncode(pid)).queryParam("label", name)
                .queryParam("logMessage", comment);
        while (true) {
            tries++;
            try {
                request.put();
                break;
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPut, e);
            }
        }
    }

    @Override
    public List<SearchResult> fieldsearch(String query, int offset, int pageLength)
            throws BackendMethodFailedException, BackendInvalidCredsException {
        try {

            ResultType searchResult = restApi.queryParam("terms", query).queryParam("maxResults", pageLength + "")
                    .queryParam("resultFormat", "xml").queryParam("pid", "true").queryParam("label", "true")
                    .queryParam("state", "true").queryParam("cDate", "true").queryParam("mDate", "true")
                    .get(ResultType.class);

            if (offset > 0) {

                for (int i = 1; i <= offset; i++) {
                    String token = searchResult.getListSession().getValue().getToken();
                    searchResult = restApi.queryParam("query", query).queryParam("sessionToken", token)
                            .queryParam("resultFormat", "xml").get(ResultType.class);
                }
            }
            List<SearchResult> outputResults = new ArrayList<SearchResult>(
                    searchResult.getResultList().getObjectFields().size());
            for (ObjectFieldsType objectFieldsType : searchResult.getResultList().getObjectFields()) {

                try {
                    outputResults.add(new SearchResult(objectFieldsType.getPid().getValue(),
                                                       objectFieldsType.getLabel().getValue(),
                                                       objectFieldsType.getState().getValue(),
                                                       DateUtils.parseDateStrict(objectFieldsType.getCDate().getValue())
                                                               .getTime(),
                                                       DateUtils.parseDateStrict(objectFieldsType.getMDate().getValue())
                                                               .getTime()));
                } catch (ParseException e) {
                    //TODO Not really a backend exception
                    throw new BackendMethodFailedException("Failed to parse date from search result", e);
                }
            }
            return outputResults;

        } catch (UniformInterfaceException e) {
            try {
                handleResponseException("search", 1, 1, e);
            } catch (BackendInvalidResourceException e1) {
                //Never happens, ignore
                throw new RuntimeException(e1);
            }
            throw e;
        }
    }

    @Override
    public Date addExternalDatastream(String pid, String datastream, String label, String url, String formatURI,
                                      String mimeType, String checksumType, String checksum, String comment)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        if (comment == null || comment.isEmpty()) {
            comment = "No message supplied";
        }

        /*
       @PathParam(RestParam.PID) String pid,
                             @PathParam(RestParam.DSID) String dsID,
                             @QueryParam(RestParam.CONTROL_GROUP) @DefaultValue("X") String controlGroup,
                             @QueryParam(RestParam.DS_LOCATION) String dsLocation,
                             @QueryParam(RestParam.ALT_IDS) List<String> altIDs,
                             @QueryParam(RestParam.DS_LABEL) String dsLabel,
                             @QueryParam(RestParam.VERSIONABLE) @DefaultValue("true") Boolean versionable,
                             @QueryParam(RestParam.DS_STATE) @DefaultValue("A") String dsState,
                             @QueryParam(RestParam.FORMAT_URI) String formatURI,
                             @QueryParam(RestParam.CHECKSUM_TYPE) String checksumType,
                             @QueryParam(RestParam.CHECKSUM) String checksum,
                             @QueryParam(RestParam.MIME_TYPE) String mimeType,
                             @QueryParam(RestParam.LOG_MESSAGE) String logMessage
        */
        WebResource resource = restApi.path("/").path(urlEncode(pid)).path("/datastreams/").path(urlEncode(datastream))
                .queryParam("controlGroup", "R").queryParam("dsLocation", url).queryParam("dsLabel", label)
                .queryParam("formatURI", formatURI).queryParam("mimeType", mimeType).queryParam("logMessage", comment);
        if (checksumType != null) {
            resource = resource.queryParam("checksumType", checksumType);
        }
        if (checksum != null) {
            resource = resource.queryParam("checksum", checksum);
        }
        int tries = 0;
        while (true) {
            tries++;
            try {
                DatastreamProfileType profile = resource.post(DatastreamProfileType.class);
                return profile.getDsCreateDate().toGregorianCalendar().getTime();
            } catch (UniformInterfaceException e) {
                handleResponseException(pid, tries, maxTriesPost, e);
            }
        }
    }

    @Override
    public Validation validate(String pid)
            throws BackendMethodFailedException, BackendInvalidCredsException, BackendInvalidResourceException {
        WebResource format = restApi.path("/").path(urlEncode(pid)).path("/validate").queryParam("format", "text/xml");
        return format.get(Validation.class);
    }

    @Override
    public String newEmptyObject(String pid, List<String> oldIDs, List<String> collections, String logMessage)
            throws BackendMethodFailedException, BackendInvalidCredsException {
        InputStream emptyObjectStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("EmptyObject.xml");
        Document emptyObject = DOM.streamToDOM(emptyObjectStream, true);

        XPathSelector xpath = DOM
                .createXPathSelector("foxml", Constants.NAMESPACE_FOXML, "rdf", Constants.NAMESPACE_RDF, "d",
                                     Constants.NAMESPACE_RELATIONS, "dc", Constants.NAMESPACE_DC, "oai_dc",
                                     Constants.NAMESPACE_OAIDC);
        //Set pid
        Node pidNode = xpath.selectNode(emptyObject, "/foxml:digitalObject/@PID");
        pidNode.setNodeValue(pid);

        Node rdfNode = xpath.selectNode(emptyObject,
                                        "/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/@rdf:about");
        rdfNode.setNodeValue("info:fedora/" + pid);

        //add Old Identifiers to DC
        Node dcIdentifierNode = xpath.selectNode(emptyObject,
                                                 "/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:identifier");
        dcIdentifierNode.setTextContent(pid);
        Node parent = dcIdentifierNode.getParentNode();
        for (String oldID : oldIDs) {
            Node clone = dcIdentifierNode.cloneNode(true);
            clone.setTextContent(oldID);
            parent.appendChild(clone);
        }

        Node collectionRelationNode = xpath.selectNode(emptyObject,
                                                       "/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/d:isPartOfCollection");

        parent = collectionRelationNode.getParentNode();
        //remove the placeholder relationNode
        parent.removeChild(collectionRelationNode);

        for (String collection : collections) {
            Node clone = collectionRelationNode.cloneNode(true);
            clone.getAttributes().getNamedItem("rdf:resource").setNodeValue("info:fedora/" + collection);
            parent.appendChild(clone);
        }

        String emptyObjectAsString;
        try {
            emptyObjectAsString = DOM.domToString(emptyObject);
        } catch (TransformerException e) {
            //TODO This is not really a backend exception
            throw new BackendMethodFailedException("Failed to convert DC back to string", e);
        }
        WebResource.Builder request = restApi.path("/").path(urlEncode(pid)).queryParam("state", "I")
                .type(MediaType.TEXT_XML_TYPE);
        int tries = 0;
        while (true) {
            tries++;
            try {
                return request.post(String.class, emptyObjectAsString);
            } catch (UniformInterfaceException e) {
                try {
                    handleResponseException(pid, tries, maxTriesPost, e);
                } catch (BackendInvalidResourceException e1) {
                    //Ignore, never happens
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    private String urlEncode(String pid) {
        try {
            return URLEncoder.encode(pid, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not known....", e);
        }
    }

    private String getAbsoluteURIAsString(String uriAsString) throws BackendMethodFailedException {
        URI predURI;
        try {
            predURI = new URI(uriAsString);
        } catch (URISyntaxException e) {
            //TODO This is not a backend exception
            throw new BackendMethodFailedException("Failed to parse uriAsString as an URI", e);
        }
        if (!predURI.isAbsolute()) {
            uriAsString = "info:fedora/" + uriAsString;
        }
        return uriAsString;
    }

    /**
     * Handle behaviour after a Fedora REST call threw an exception.
     * Will rethrow as a different specific exception on authorization errors or not found errors, and conflicts.
     * In case of conflict, the method may instead log a warning, wait a short while and return without errors, thus
     * allowing the calling method to retry. This will happen if the "tries" parameter is less than the "maxTries"
     * parameter. The delay is done with exponential backoff, first waiting {@link #retryDelay}, and
     * 2*{@link #retryDelay}, then 4*{@link #retryDelay} and so forth.
     *
     * @param pid      The PID of the object worked on. Used for giving context to the exception error messages.
     * @param tries    The number of times this call has been tried already.
     * @param maxTries The maximum number of times this call should be retried
     * @param e        The exception that happened.
     * @throws BackendInvalidCredsException    If the exception represents a 401 error.
     * @throws BackendInvalidResourceException If the exception represents a 404 error.
     * @throws ConcurrentModificationException If the exception represents a 409 error and should not be retried.
     * @throws BackendMethodFailedException    On any other error
     */
    private void handleResponseException(String pid, int tries, int maxTries, UniformInterfaceException e)
            throws BackendInvalidCredsException, BackendInvalidResourceException, BackendMethodFailedException {
        ClientResponse response = e.getResponse();
        switch (response.getClientResponseStatus()) {
            case UNAUTHORIZED:
                throw new BackendInvalidCredsException("Invalid Credentials Supplied: pid '" + pid + "'", e);
            case NOT_FOUND:
                throw new BackendInvalidResourceException("Resource '" + pid + "'not found", e);
            case BAD_REQUEST:
                throw new BackendMethodFailedException(response.getEntity(String.class), e);
            case CONFLICT:
                // URLConnection will sometimes retry doing the same operation due to a timeout or an error.
                // In those cases the object will often be locked on the second try.
                // In that case we try again, we can retry after a delay with exponential backoff for a specified
                // amount of times and a specified number of retries.
                if (tries < maxTries) {
                    try {
                        int delay = retryDelay * (1 << (tries - 1));
                        log.warn("Fedora returned 409. Retrying after '" + delay + "' milliseconds", e);
                        Thread.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        //Ignore
                    }
                    break;
                } else {
                    throw new ConcurrentModificationException("Object locked when trying to set state", e);
                }
            default:
                throw new BackendMethodFailedException("Server error for '" + pid + "', " + response.toString(), e);
        }
    }
}