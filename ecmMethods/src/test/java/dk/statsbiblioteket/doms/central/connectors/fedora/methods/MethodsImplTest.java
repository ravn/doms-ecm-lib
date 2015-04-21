package dk.statsbiblioteket.doms.central.connectors.fedora.methods;

import org.junit.Test;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.fedora.FedoraRest;
import dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Method;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA. User: abr Date: 9/13/12 Time: 11:30 AM To change this template use File | Settings | File
 * Templates.
 */
public class MethodsImplTest {

    private static final String PID = "uuid:199a400f-1c5a-41b1-9be1-d448e8cb3c50";
    private static final String CM_PID = "doms:ContentModel_Test";
    private static final String METHODS = "METHODS";

    @org.junit.Test
    public void testGetMethods() throws Exception {
        //Set up fixture
        FedoraRest fedora = mock(FedoraRest.class);
        when(fedora.getObjectProfile(PID, null)).thenReturn(createObjectProfile());
        when(fedora.getXMLDatastreamContents(CM_PID, METHODS, null)).thenReturn(createMethodsDatastream());
        when(fedora.getUsername()).thenReturn("user");
        when(fedora.getPassword()).thenReturn("pass");
        Methods ms = new MethodsImpl(fedora, "http://localhost:7880/fedora");

        //Call method
        List<Method> methods = ms.getDynamicMethods(PID, null);

        //Assume pobject profile is read to find the content model
        verify(fedora).getObjectProfile(PID, null);
        //Assume the METHODS datastream of the content model is read
        verify(fedora).getXMLDatastreamContents(CM_PID, METHODS, null);
        //Nothing else should happen
        verifyNoMoreInteractions(fedora);

        //Check result
        assertEquals("Should find one method", 1, methods.size());
        assertEquals("Should find expected method", "Test Method", methods.get(0).getName());
        assertEquals("Should find expected method", "echo pid: %%domsPid%% key: %%key%%", methods.get(0).getCommand());
        assertEquals("Should find expected method", "dynamic", methods.get(0).getType());
        assertEquals("Should find expected method", 1, methods.get(0).getParameters().getParameter().size());
        assertEquals("Should find expected method", "ServerFile",
                     methods.get(0).getParameters().getParameter().get(0).getType());
        assertEquals("Should find expected method", "key",
                     methods.get(0).getParameters().getParameter().get(0).getName());
        assertEquals("Should find expected method", "/tmp",
                     methods.get(0).getParameters().getParameter().get(0).getConfig());
        assertEquals("Should find expected method", null,
                     methods.get(0).getParameters().getParameter().get(0).getDefault());
        assertEquals("Should find expected method", null,
                     methods.get(0).getParameters().getParameter().get(0).getParameterprefix());
    }

    @Test
    public void testInvokeMethod() throws Exception {
        //Set up fixture
        FedoraRest fedora = mock(FedoraRest.class);
        when(fedora.getObjectProfile(PID, null)).thenReturn(createObjectProfile());
        when(fedora.getXMLDatastreamContents(CM_PID, METHODS, null)).thenReturn(createMethodsDatastream());
        when(fedora.getXMLDatastreamContents(PID, METHODS, null)).thenThrow(new BackendInvalidResourceException("Unknown resource"));
        when(fedora.getUsername()).thenReturn("user");
        when(fedora.getPassword()).thenReturn("pass");
        Methods ms = new MethodsImpl(fedora, "http://localhost:7880/fedora");

        //Call method
        Map<String, List<String>> values = new HashMap<String, List<String>>();
        values.put("key", Arrays.asList("value"));
        String result = ms.invokeMethod(PID, "Test Method", values, null);

        //Assume pobject profile is read to find the content model
        verify(fedora).getObjectProfile(PID, null);
        //Assume the METHODS datastream of the object is tried
        verify(fedora).getXMLDatastreamContents(PID, METHODS, null);
        //Assume the METHODS datastream of the content model is read
        verify(fedora).getXMLDatastreamContents(CM_PID, METHODS, null);
        //The method may insert username
        verify(fedora).getUsername();
        //The method may insert password
        verify(fedora).getPassword();
        //Nothing else should happen
        verifyNoMoreInteractions(fedora);

        assertEquals("Should give expected result", "pid: " + PID + " key: value\n", result);
    }

    private String createMethodsDatastream() {
        return "<methods xmlns=\"http://doms.statsbiblioteket.dk/types/methods/default/0/1/#\">\n"
                + "  <method>\n"
                + "    <name>Test Method</name>\n"
                + "    <command>echo pid: %%domsPid%% key: %%key%%</command>\n"
                + "    <type>dynamic</type>\n"
                + "    <parameters>\n"
                + "      <parameter>\n"
                + "        <name>key</name>\n"
                + "        <type>ServerFile</type>\n"
                + "        <config>/tmp</config>\n"
                + "      </parameter>\n"
                + "    </parameters>\n"
                + "  </method>\n"
                + "</methods>\n";
    }

    private dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile createObjectProfile() {
        dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile objectProfile
                = new dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile();
        objectProfile.setPid(PID);
        objectProfile.setObjectCreatedDate(new Date());
        objectProfile.setObjectLastModifiedDate(new Date());
        objectProfile.setContentModels(Arrays.asList(CM_PID));
        objectProfile.setLabel("label");
        objectProfile.setOwnerID("owner");
        objectProfile.setState("A");
        return objectProfile;
    }
}
