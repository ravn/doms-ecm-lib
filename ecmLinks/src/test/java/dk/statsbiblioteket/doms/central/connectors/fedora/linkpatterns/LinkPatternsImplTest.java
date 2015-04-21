package dk.statsbiblioteket.doms.central.connectors.fedora.linkpatterns;

import dk.statsbiblioteket.doms.central.connectors.fedora.FedoraRest;
import dk.statsbiblioteket.sbutil.webservices.configuration.ConfigCollection;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/18/13 Time: 10:16 AM To change this template use File | Settings | File
 * Templates.
 */
public class LinkPatternsImplTest {

    private static final String PID = "uuid:199a400f-1c5a-41b1-9be1-d448e8cb3c50";
    private static final String CM_PID = "doms:ContentModel_Test";
    private static final String LINK_PATTERN = "LINK_PATTERN";
    private static final String DATASTREAM = "DATASTREAM";

    @Test
    public void testGetLinkPatterns() throws Exception {
        //Set up fixture
        FedoraRest fedora = mock(FedoraRest.class);
        when(fedora.getObjectProfile(PID, 9999L)).thenReturn(createObjectProfile());
        when(fedora.getXMLDatastreamContents(CM_PID, LINK_PATTERN, 9999L)).thenReturn(createLinkPatterns());
        when(fedora.getXMLDatastreamContents(PID, DATASTREAM, 9999L)).thenReturn(createDatastream());
        when(fedora.getUsername()).thenReturn("user");
        when(fedora.getPassword()).thenReturn("pass");
        ConfigCollection.getProperties().setProperty("contextParam.p1", "localhost");
        LinkPatterns lp = new LinkPatternsImpl(fedora, "http://localhost:7880/fedora");

        //Call method
        List<LinkPattern> patterns = lp.getLinkPatterns(PID, 9999L);

        //Assume object profile is read to find the content model
        verify(fedora).getObjectProfile(PID, 9999L);
        //Assume the LINK_PATTERN datastream of the content model is read
        verify(fedora).getXMLDatastreamContents(CM_PID, LINK_PATTERN, 9999L);
        //The link patterns tells us to read DATASTREAM for one specific parameter, assume that is read
        verify(fedora).getXMLDatastreamContents(PID, DATASTREAM, 9999L);
        //The links may insert username
        verify(fedora).getUsername();
        //The links may insert password
        verify(fedora).getPassword();
        //Nothing else should happen
        verifyNoMoreInteractions(fedora);

        //Check result
        assertTrue(patterns.size() == 1);
        assertEquals("Should find expected link", "Test Link", patterns.get(0).getName());
        assertEquals("Should find expected link", "Test Link Description", patterns.get(0).getDescription());
        assertEquals("Should find expected link",
                     "http://localhost/x?a=uuid%3A199a400f-1c5a-41b1-9be1-d448e8cb3c50&b=value",
                     patterns.get(0).getValue());
    }

    private String createDatastream() {
        return "<x><y>value</y></x>";
    }

    private String createLinkPatterns() {
        return "<linkPatterns xmlns=\"http://doms.statsbiblioteket.dk/types/linkpattern/0/1/#\">\n"
                + "   <linkPattern>\n"
                + "      <name>Test Link</name>\n"
                + "      <description>Test Link Description</description>\n"
                + "      <value>http://{contextParam.p1}/x?a={domsPid}&amp;b={key}</value>\n"
                + "      <replacements>\n"
                + "        <replacement>\n"
                + "           <key>key</key>\n"
                + "           <datastream>DATASTREAM</datastream>\n"
                + "           <xpath>x/y</xpath>\n"
                + "        </replacement>\n"
                + "      </replacements>\n"
                + "   </linkPattern>\n"
                + "</linkPatterns>\n";
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
