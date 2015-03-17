package dk.statsbiblioteket.doms.central.connectors.fedora.fedoraDBsearch;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/15/12 Time: 12:52 PM To change this template use File | Settings | File
 * Templates.
 */
public interface DBSearch {


    List<String> listObjectsWithThisLabel(String label)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException;


    List<String> findObjectFromDCIdentifier(String string)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException;

}
