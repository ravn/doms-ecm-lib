package dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator;

import com.sun.jersey.api.client.Client;

import javax.ws.rs.core.MediaType;

/**
 * Created by IntelliJ IDEA. User: abr Date: 3/29/12 Time: 2:55 PM To change this template use File | Settings | File
 * Templates.
 */
public class PidGeneratorImpl implements PidGenerator {

    private final Client restClient;
    private String pidGenLocation;


    public PidGeneratorImpl(String pidGenLocation)
            throws
            PIDGeneratorException {
        this.pidGenLocation = pidGenLocation;


        restClient = Client.create();

    }

    /**
     * Generate the next available PID.
     *
     * @param infix A string, all or part of which may be used as part of the PID, but with no guarantee. May be left
     *              empty.
     *
     * @return The next available (unique) PID, possibly including (part of) the requested infix.
     */
    public String generateNextAvailablePID(String infix) {

        if (infix == null) {
            infix = "";
        }

        return restClient.resource(pidGenLocation)
                         .path("rest/pids/generatePid/")
                         .path(infix)
                         .accept(MediaType.TEXT_PLAIN)
                         .get(String.class);

    }
}
