package dk.statsbiblioteket.doms.central.connectors.fedora.templates;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:31 PM To change this template use File | Settings | File
 * Templates.
 */
public interface Templates {


    public String cloneTemplate(String templatepid,
                                List<String> oldIDs,
                                String logMessage)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            ObjectIsWrongTypeException,
            BackendInvalidResourceException,
            PIDGeneratorException;
}
