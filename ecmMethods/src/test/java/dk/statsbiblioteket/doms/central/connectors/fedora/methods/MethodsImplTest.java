package dk.statsbiblioteket.doms.central.connectors.fedora.methods;

import dk.statsbiblioteket.doms.central.connectors.fedora.FedoraRest;
import dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Method;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import org.junit.Ignore;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA. User: abr Date: 9/13/12 Time: 11:30 AM To change this template use File | Settings | File
 * Templates.
 */
public class MethodsImplTest {
    @org.junit.Test @Ignore
    public void testGetMethods()
            throws
            Exception {
        MethodsImpl
                methods =
                new MethodsImpl(new FedoraRest(new Credentials("fedoraAdmin", "fedoraAdminPass"),
                                               "http://alhena:7880/fedora"), "");
        List<Method> methodList = methods.getStaticMethods("doms:ContentModel_VHSFile", 0l);
        assertTrue("no methods!", methodList.size() > 0);
    }


}
