package dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:52 PM To change this template use File | Settings | File
 * Templates.
 */
public class PIDGeneratorException extends Exception {

    public PIDGeneratorException(String message) {
        super(message);
    }

    public PIDGeneratorException(String message,
                                 Throwable cause) {
        super(message, cause);
    }
}
