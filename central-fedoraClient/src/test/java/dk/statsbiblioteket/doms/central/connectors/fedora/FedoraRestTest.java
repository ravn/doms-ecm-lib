package dk.statsbiblioteket.doms.central.connectors.fedora;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.Validation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.DatastreamProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.util.Strings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class FedoraRestTest {

    @org.junit.Test
    @Ignore
    public void testNewEmptyObject() throws Exception {
        FedoraRest fedora = new FedoraRest(
                new Credentials("fedoraAdmin", "fedoraAdminPass"), "http://achernar:7880/fedora");
        final String pid1 = "uuid:testPid" + new Date().getTime();
        String pid = fedora.newEmptyObject(pid1, Arrays.asList("oldIdentfier1"), Arrays.asList("uuid:Batch"), "message");
        assertEquals(pid,pid1);
    }

    @org.junit.Test
    @Ignore
    public void testValidate() throws Exception {
        FedoraRest fedora = new FedoraRest(
                new Credentials("fedoraAdmin", "fedoraAdminPass"), "http://achernar:7880/fedora");
        String pid = fedora.newEmptyObject(
                "uuid:testPid", Arrays.asList("oldIdentfier1"), Arrays.asList("uuid:Batch"), "message");
        assertEquals(pid,"uuid:testPid");
        Validation validation = fedora.validate(pid);
        assertTrue(validation.isValid());
    }

    @org.junit.Test
    @Ignore
    public void testAddRelations() throws
                                   MalformedURLException,
                                   BackendInvalidCredsException,
                                   BackendMethodFailedException, BackendInvalidResourceException {
        FedoraRest fedora = new FedoraRest(
                new Credentials("fedoraAdmin", "fedoraAdminPass"), "http://achernar:7880/fedora");
        String pid = "uuid:testPid2";
        try {

            fedora.newEmptyObject(
                    pid, Arrays.asList("oldIdentfier1"), Arrays.asList("uuid:Batch"), "message");
        } catch (Exception e){

        }
        fedora.addRelations(pid,null, Constants.RELATION_COLLECTION,Arrays.asList("uuid:test2","uuid:test3"),false,"comment");

    }
    
    @Ignore
    @org.junit.Test
    public void testModifyDatastream() throws
            MalformedURLException,
            BackendInvalidCredsException,
            BackendMethodFailedException, BackendInvalidResourceException {
        FedoraRest fedora = new FedoraRest(
                new Credentials("fedoraAdmin", "fedoraAdminPass"), "http://achernar:7880/fedora");
        String pid = "uuid:testPid2";
        try {

            fedora.newEmptyObject(
                    pid, Arrays.asList("oldIdentfier1"), Arrays.asList("uuid:Batch"), "message");
        } catch (Exception e){

        }
        fedora.modifyDatastreamByValue(pid, "STREAM", null, null, "<foobar/>".getBytes(), null, "Hello World", null);
        DatastreamProfile profile = fedora.getDatastreamProfile(pid, "STREAM", null);
        try {
            fedora.modifyDatastreamByValue(pid, "STREAM", null, null, "<foobar>barfoo</foobar".getBytes(), null, "Hello World", profile.getCreated()/1000L - 10L);
            fail("Should throw " + ConcurrentModificationException.class.getSimpleName());
        } catch (ConcurrentModificationException e) {
            //expected
        }
    }
    @Test
    public void testInlineDatastreams() throws
                                        IOException,
                                        TransformerException,
                                        BackendInvalidResourceException,
                                        BackendMethodFailedException,
                                        BackendInvalidCredsException, ParseException {
        String xml = Strings.flush(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("sampleExport.xml"));
        FedoraRest fedoraRest = Mockito.mock(FedoraRest.class);
        String value = "<testContent/>";
        when(fedoraRest.getXMLDatastreamContents(anyString(), anyString(), anyLong())).thenReturn(value);
        long timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse("2014-01-12T05:04:59.583Z")
                                                                             .getTime();
        ObjectXml objectXml = new ObjectXml("test", xml, fedoraRest, timestamp);


        xml = objectXml.getCleaned();
        Assert.assertFalse(xml.contains("<foxml:contentLocation TYPE=\"INTERNAL_ID\""));
        assertTrue(xml.contains(value));
        Assert.assertFalse(xml.contains("<foxml:datastream ID=\"AUDIT\""));
        assertTrue(xml.contains("ID=\"BATCHSTRUCTURE.43\""));
        Assert.assertFalse(xml.contains("ID=\"BATCHSTRUCTURE.44\""));
    }

}
