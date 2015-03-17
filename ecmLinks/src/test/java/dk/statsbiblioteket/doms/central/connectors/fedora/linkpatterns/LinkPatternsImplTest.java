package dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns;

import dk.statsbiblioteket.doms.central.connectors.fedora.FedoraRest;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/18/13 Time: 10:16 AM To change this template use File | Settings | File
 * Templates.
 */
public class LinkPatternsImplTest {
    @Test @Ignore
    public void testGetLinkPatterns()
            throws
            Exception {
        String fedoraLocation = "http://alhena:7880/fedora";

        LinkPatterns
                lp =
                new LinkPatternsImpl(new FedoraRest(new Credentials("fedoraAdmin", "fedoraAdminPass"), fedoraLocation),
                                     fedoraLocation);


        List<LinkPattern> patterns = lp.getLinkPatterns("uuid:199a400f-1c5a-41b1-9be1-d448e8cb3c50", null);
        assertTrue(patterns.size() > 0);
        System.out.println(patterns.get(0).getValue());


    }
}
