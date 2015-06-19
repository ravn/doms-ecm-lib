package dk.statsbiblioteket.doms.central.connectors.fedora;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.generated.Validation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.DatastreamProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.utils.Constants;
import dk.statsbiblioteket.sbutil.webservices.authentication.Credentials;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @org.junit.Test
    @Ignore
    public void testGetNamedRelations() throws
                                        MalformedURLException,
                                        BackendInvalidCredsException,
                                        BackendMethodFailedException,
                                        BackendInvalidResourceException,
                                        InterruptedException {
        FedoraRest fedora = new FedoraRest(new Credentials("fedoraAdmin", "fedoraAdminPass"), "http://achernar:7880/fedora");
        String pid = "uuid:testPid5";
        fedora.newEmptyObject(pid, Arrays.asList("oldIdentfier5"), Arrays.asList("uuid:Batch"), "message");

        while (true) {//Clean all relations already existing,
            List<FedoraRelation> relations = fedora.getNamedRelations(pid, Constants.RELATION_COLLECTION, null);
            if (relations.isEmpty()){
                break;
            }
            for (FedoraRelation relation : relations) {
                fedora.deleteRelation(pid, relation.getSubject(), relation.getPredicate(), relation.getObject(), false, "comment");
            }
        }
        Thread.sleep(1000);
        Date before = new Date(); //Take a timestamp for when the object is clean
        Thread.sleep(1000);
        fedora.addRelation(pid, null, Constants.RELATION_COLLECTION, "uuid:test2", false, "comment");
        Thread.sleep(1000);
        Date middle = new Date(); //Timestamp for just one relation
        Thread.sleep(1000);
        fedora.addRelation(pid, null, Constants.RELATION_COLLECTION, "uuid:test3", false, "comment");
        Thread.sleep(1000);

        List<FedoraRelation> relationsAfter = fedora.getNamedRelations(pid, Constants.RELATION_COLLECTION, new Date().getTime());
        assertEquals(relationsAfter.toString(),2,relationsAfter.size());

        List<FedoraRelation> relationsBefore = fedora.getNamedRelations(pid, Constants.RELATION_COLLECTION, before.getTime());
        assertEquals(relationsBefore.toString(),0,relationsBefore.size());

        List<FedoraRelation> relationsMiddle = fedora.getNamedRelations(pid, Constants.RELATION_COLLECTION, middle.getTime());
        assertEquals(relationsMiddle.toString(),1,relationsMiddle.size());
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
        String xml = Strings.flush(Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream("sampleExport.xml"));
        FedoraRest fedoraRest = Mockito.mock(FedoraRest.class);
        String value = "<testContent/>";
        when(fedoraRest.getXMLDatastreamContents(anyString(), anyString(), anyLong())).thenReturn(value);
        long timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse("2014-01-12T05:04:59.583Z")
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
