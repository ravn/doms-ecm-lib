package dk.statsbiblioteket.doms.central.connectors.fedora.methods;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Method;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: abr Date: 9/13/12 Time: 10:54 AM To change this template use File | Settings | File
 * Templates.
 */
public interface Methods {

    /**
     * List all static methods for a given content model.
     *
     * @param cmpid    Pid of the content model.
     * @param asOfTime Use the methods defined at this time (unix time in ms), or null for now.
     *
     * @return List of methods defined.
     * @throws BackendInvalidCredsException If current credentials provided are invalid.
     * @throws BackendMethodFailedException If communicating with Fedora failed.
     * @throws BackendInvalidResourceException
     *                                      If content model doesn't exist.
     */
    public List<Method> getStaticMethods(String cmpid,
                                         Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException;

    /**
     * List all dynamic methods for a given object.
     *
     * @param objpid   Pid of the object.
     * @param asOfTime Use the methods defined at this time (unix time in ms), or null for now.
     *
     * @return List of methods defined.
     * @throws BackendInvalidCredsException If current credentials provided are invalid.
     * @throws BackendMethodFailedException If communicating with Fedora failed.
     * @throws BackendInvalidResourceException
     *                                      If object doesn't exist.
     */
    public List<Method> getDynamicMethods(String objpid,
                                          Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException;

    /**
     * Invoke a given method with the given parameters.
     *
     * @param pid        The pid of the content model or object defining the method.
     * @param methodName The name of the method.
     * @param parameters Parameters for the method, as a map from name list of values.
     * @param asOfTime   Use the methods defined at this time (unix time in ms), or null for now.
     *
     * @return Result of calling method.
     * @throws BackendInvalidCredsException If current credentials provided are invalid.
     * @throws BackendMethodFailedException If communicating with Fedora failed.
     * @throws BackendInvalidResourceException
     *                                      If object, content model or method doesn't exist.
     */
    public String invokeMethod(String pid,
                               String methodName,
                               Map<String, List<String>> parameters,
                               Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException;
}
