package dk.statsbiblioteket.doms.central.connectors.fedora.fedoraDBsearch;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.Connector;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/15/12 Time: 12:49 PM To change this template use File | Settings | File
 * Templates.
 */
public class DBSearchRest extends Connector implements DBSearch {

    private static final
    Pattern
            PIDPATTERN =
            Pattern.compile(Pattern.quote("<pid>") + "([^<]*)" + Pattern.quote("</pid>"));

    private WebResource restApi;

    private static Log log = LogFactory.getLog(DBSearchRest.class);


    public DBSearchRest(Credentials creds,
                        String location)
            throws
            MalformedURLException {
        super(creds, location);
        restApi = client.resource(location + "/objects");
        restApi.addFilter(new HTTPBasicAuthFilter(creds.getUsername(), creds.getPassword()));
    }


    @Override
    public List<String> findObjectFromDCIdentifier(String identifier)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException {
        //http://alhena:7980/fedora/objects?pid=true&title=true&identifier=true&terms=&query=identifier~129447RitzauProgram&maxResults=1&resultFormat=xml
        WebResource
                query =
                restApi.queryParam("pid", "true")
                       .queryParam("query", "identifier=" + identifier)
                       .queryParam("maxResults", "1")
                       .queryParam("resultFormat", "xml");
        return genericQuery(query);
    }


    public List<String> listObjectsWithThisLabel(String label)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException {
        WebResource
                query =
                restApi.queryParam("pid", "true")
                       .queryParam("query", "label=" + label)
                       .queryParam("maxResults", "1")
                       .queryParam("resultFormat", "xml");
        return genericQuery(query);
    }


    /**
     * Runs any query, that produces one column of results, and return each line as a string
     *
     * @param query The query to execute
     *
     * @return an empty list
     */
    public List<String> genericQuery(WebResource query)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException {
        try {
            String result = query.get(String.class);
            Matcher matcher = PIDPATTERN.matcher(result);
            String pid = null;
            List<String> resultList = new ArrayList<String>();
            while (matcher.find()) {
                pid = matcher.group(1);
                resultList.add(pid);
            }
            return resultList;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == ClientResponse.Status.UNAUTHORIZED.getStatusCode()) {
                throw new BackendInvalidCredsException("Invalid Credentials Supplied", e);
            } else {
                throw new BackendMethodFailedException("Server error", e);
            }
        }


    }

}
