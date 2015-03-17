package dk.statsbiblioteket.doms.central.connectors.fedora.views;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:33 PM To change this template use File | Settings | File
 * Templates.
 */
public interface Views {


    /**
     * Get a list of the objects in the view of a given object
     *
     * @param objpid    the object whose view we examine
     * @param viewAngle The view angle
     * @param asOfTime
     *
     * @return the list of the pids in the view of objpid
     */
    public List<String> getViewObjectsListForObject(String objpid,
                                                    String viewAngle,
                                                    Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException;

    /**
     * Get a bundle of the xml dump of the objects in the view of objpid. The objects will be bundled under the supertag
     * dobundle:digitalObjectBundle, where dobundle is defined in Constants
     *
     * @param objpid    the object whose view we examine
     * @param viewAngle The view angle
     * @param asOfTime
     *
     * @return The objects bundled under the supertag
     * @see dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants#NAMESPACE_DIGITAL_OBJECT_BUNDLE
     */
    public Document getViewObjectBundleForObject(String objpid,
                                                 String viewAngle,
                                                 Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException;

    /**
     * Given an object, find view angles this object is an entry object for
     * @param pid the object to find entry views for
     * @param asOfTime
     * @return Entry views
     * @throws BackendInvalidCredsException
     * @throws BackendMethodFailedException
     * @throws BackendInvalidResourceException
     */
    Set<String> determineEntryAngles(String pid, Long asOfTime)
            throws BackendInvalidCredsException, BackendMethodFailedException, BackendInvalidResourceException;
}
