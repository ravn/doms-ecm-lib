package dk.statsbiblioteket.doms.central.connectors.fedora.templates;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:48 PM To change this template use File | Settings | File
 * Templates.
 */
public class ObjectIsWrongTypeException extends Exception {
    public ObjectIsWrongTypeException(String message) {
        super(message);
    }

    public ObjectIsWrongTypeException(String message,
                                      Throwable cause) {
        super(message, cause);
    }
}
