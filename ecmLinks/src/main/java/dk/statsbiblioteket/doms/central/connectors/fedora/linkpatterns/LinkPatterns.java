package dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/15/13 Time: 2:17 PM To change this template use File | Settings | File
 * Templates.
 */
public interface LinkPatterns {

    public List<LinkPattern> getLinkPatterns(String pid,
                                             Long asOfDate)
            throws
            BackendInvalidResourceException,
            BackendInvalidCredsException,
            BackendMethodFailedException;
}
