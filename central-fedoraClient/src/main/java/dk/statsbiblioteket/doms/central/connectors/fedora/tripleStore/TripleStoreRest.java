package dk.statsbiblioteket.doms.central.connectors.fedora.tripleStore;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.Connector;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/15/12 Time: 12:49 PM To change this template use File | Settings | File
 * Templates.
 */
public class TripleStoreRest extends Connector implements TripleStore {


    private final Fedora fedora;
    private WebResource restApi;

    private static Log log = LogFactory.getLog(TripleStoreRest.class);


    public TripleStoreRest(Credentials creds, String location, Fedora fedora) throws MalformedURLException {
        super(creds, location);
        this.fedora = fedora;
        restApi = client.resource(location + "/risearch")
                        .queryParam("type", "triples")
                        .queryParam("lang", "spo")
                        .queryParam("format", "N-Triples")
                        .queryParam("stream", "on");
        restApi.addFilter(new HTTPBasicAuthFilter(creds.getUsername(), creds.getPassword()));
    }


    @Override
    public List<FedoraRelation> getInverseRelations(String pid, String predicate) throws
                                                                                  BackendMethodFailedException,
                                                                                  BackendInvalidCredsException,
                                                                                  BackendInvalidResourceException {
        try {


            String subject = toUri(pid);

            String queryStart = "* ";

            String predicateQuery;
            if (predicate == null || predicate.isEmpty()) {
                predicateQuery = " * ";
            } else {
                predicateQuery = " <" + predicate + "> ";
            }

            String query = queryStart + predicateQuery + " <" + subject + "> ";
            String objects = restApi.queryParam("query", query).post(String.class);
            String[] lines = objects.split("\n");
            List<FedoraRelation> relations = new ArrayList<FedoraRelation>();

            for (String line : lines) {
                if (line.startsWith("\"")) {
                    continue;
                }
                if (line.trim().isEmpty()){
                    continue;
                }
                String[] components = line.split(" ");
                if (predicateQuery.equals(" * ")) {
                    relations.add(new FedoraRelation(toPid(components[0]), clean(components[1]), pid));
                } else {
                    relations.add(new FedoraRelation(toPid(components[0]), predicate, pid));
                }
            }
            return relations;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == ClientResponse.Status.UNAUTHORIZED.getStatusCode()) {
                throw new BackendInvalidCredsException("Invalid Credentials Supplied", e);
            } else if (e.getResponse().getStatus() == ClientResponse.Status.NOT_FOUND.getStatusCode()) {
                throw new BackendInvalidResourceException("Resource not found", e);
            } else {
                throw new BackendMethodFailedException("Server error", e);
            }
        }
    }


    @Override
    public void flushTriples() throws BackendInvalidCredsException, BackendMethodFailedException {
    }


    @Override
    public List<String> getContentModelsInCollection(String collectionPid) throws
                                                                                          BackendInvalidCredsException,
                                                                                          BackendMethodFailedException {
        List<FedoraRelation> allContentModels = genericQuery("* <info:fedora/fedora-system:def/model#hasModel> <info:fedora/fedora-system:ContentModel-3.0>");
        List<String> selectedContentModels = new ArrayList<String>();
        for (FedoraRelation relation : allContentModels) {
            String contentModel = toPid(relation.getSubject());
            try {
                ObjectProfile profile = fedora.getObjectProfile(contentModel, null);
                if (profile.getState().equals("A") || profile.getState().equals("I")) {
                    boolean isPartOfCollection = false;
                    for (FedoraRelation fedoraRelation : profile.getRelations()) {
                        if (fedoraRelation.getPredicate().equals(Constants.RELATION_COLLECTION) && fedoraRelation.getObject()
                                                                                                                 .equals(collectionPid)) {
                            isPartOfCollection = true;
                            break;
                        }
                    }
                    if (isPartOfCollection){
                        selectedContentModels.add(contentModel);
                    }
                }

            } catch (BackendInvalidResourceException e) {
                continue;
            }

        }
        return selectedContentModels;
    }


    /**
     * Runs any query, that produces one column of results, and return each line as a string
     *
     * @param query The query to execute
     *
     * @return an empty list
     */
    public List<FedoraRelation> genericQuery(String query) throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            String objects = restApi.queryParam("query", query).post(String.class);
            String[] lines = objects.split("\n");
            List<FedoraRelation> foundobjects = new ArrayList<FedoraRelation>();
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] components = line.split(" ");
                foundobjects.add(new FedoraRelation(toPid(components[0]),clean(components[1]),toPid(components[2])));
            }
            return foundobjects;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == ClientResponse.Status.UNAUTHORIZED.getStatusCode()) {
                throw new BackendInvalidCredsException("Invalid Credentials Supplied", e);
            } else {
                throw new BackendMethodFailedException("Server error", e);
            }
        }
    }
}
