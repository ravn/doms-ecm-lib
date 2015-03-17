package dk.statsbiblioteket.doms.central.connectors.fedora.utils;

/**
 * Created by IntelliJ IDEA. User: abr Date: Nov 12, 2009 Time: 4:30:39 PM To change this template use File | Settings |
 * File Templates.
 */
public class FedoraUtil {


    /**
     * If the given string starts with "info:fedora/", remove it.
     *
     * @param pid A pid, possibly as a URI
     *
     * @return The pid, with the possible URI prefix removed.
     */
    public static String ensurePID(String pid) {
        if (pid.startsWith(FEDORA_URI_PREFIX)) {
            pid = pid.substring(FEDORA_URI_PREFIX.length());
        }
        return pid;
    }

    /**
     * If the given string does not start with "info:fedora/", add it.
     *
     * @param uri An URI, possibly as a PID
     *
     * @return The uri, with the possible URI prefix prepended.
     */
    public static String ensureURI(String uri) {
        if (!uri.startsWith(FEDORA_URI_PREFIX)) {
            uri = FEDORA_URI_PREFIX + uri;
        }
        return uri;
    }


    /**
     * The static fedora uri prefix, to convert between pids and uris
     */
    private static final String FEDORA_URI_PREFIX = "info:fedora/";

}
