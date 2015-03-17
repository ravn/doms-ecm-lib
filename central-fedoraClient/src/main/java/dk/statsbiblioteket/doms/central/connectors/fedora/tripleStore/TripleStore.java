package dk.statsbiblioteket.doms.central.connectors.fedora.tripleStore;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/15/12 Time: 12:52 PM To change this template use File | Settings | File
 * Templates.
 */
public interface TripleStore {


    List<String> getContentModelsInCollection(String collectionPid)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException;


    void flushTriples()
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException;


    List<FedoraRelation> getInverseRelations(String pid,
                                             String predicate)
            throws
            BackendMethodFailedException,
            BackendInvalidCredsException,
            BackendInvalidResourceException;


    /**
     * Runs any query, that produces one column of results, and return each line as a string
     *
     * @param query The query to execute
     *
     * @return an empty list
     */
    public List<FedoraRelation> genericQuery(String query)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException;
}
