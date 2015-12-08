package dk.statsbiblioteket.doms.central.connectors.fedora.tripleStore;

import com.sun.jersey.api.client.WebResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectType;
import dk.statsbiblioteket.sbutil.webservices.authentication.Credentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Basic tests of the triplestore API
 */
public class TripleStoreRestTest {

    private TripleStoreRest ts;
    private Fedora mockFedora;
    private WebResource mockWebResource;

    /**
     * Common fixture: A TripleStoreRest object based on a mock fedora and a mock WebResource.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        mockFedora = mock(Fedora.class);
        mockWebResource = mock(WebResource.class);
        ts = new TripleStoreRest(new Credentials("user", "pass"), "http://localhost:7880/fedora", mockFedora);

        //TripleStoreRest is not easily testable, as it initiates its own Jersey webresource.
        //We override this by directly setting the variable after initialisation
        //FIXME Consider decoupling REST interaction from logic, and take REST class as constructor parameter
        ts.restApi = mockWebResource;
        when(mockWebResource.queryParam(anyString(), anyString())).thenReturn(mockWebResource);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test inverse relations.
     *
     * @throws Exception
     */
    @Test
    public void testGetInverseRelations() throws Exception {
        //Fixture: Return one known relation
        when(mockWebResource.post(String.class))
                .thenReturn("info:fedora/doms:Subject doms:whatsUp info:fedora/doms:Object");

        //Call method
        List<FedoraRelation> inverseRelations = ts.getInverseRelations("doms:Object", "doms:whatsUp");

        //Check calls-
        verify(mockWebResource).queryParam(eq("query"), matches("\\* *<doms:whatsUp> *<info:fedora/doms:Object> *"));
        verify(mockWebResource).post(String.class);
        verifyNoMoreInteractions(mockFedora, mockWebResource);

        //Check result
        assertEquals("Should return the one expected relation.", Collections.singletonList(
                new FedoraRelation("info:fedora/doms:Subject", "doms:whatsUp", "info:fedora/doms:Object")),
                     inverseRelations);
    }

    /**
     * Test flushing triples.
     *
     * @throws Exception
     */
    @Test
    public void testFlushTriples() throws Exception {
        //Call method
        ts.flushTriples();

        //This should do nothing
        verifyNoMoreInteractions(mockFedora, mockWebResource);
    }

    /**
     * Test content models in collection.
     *
     * @throws Exception
     */
    @Test
    public void testGetContentModelsInCollection() throws Exception {
        //Fixture: Return two known relations to content models. One is part of the collection, the other isn't
        when(mockWebResource.post(String.class)).thenReturn(""
                        + "info:fedora/doms:CM1 "
                        + "info:fedora/fedora-system:def/model#hasModel "
                        + "info:fedora/fedora-system:ContentModel-3.0\n"
                        + "info:fedora/doms:CM2 "
                        + "info:fedora/fedora-system:def/model#hasModel "
                        + "info:fedora/fedora-system:ContentModel-3.0");
        when(mockFedora.getObjectProfile("doms:CM1", null)).thenReturn(createCM1Profile());
        when(mockFedora.getObjectProfile("doms:CM2", null)).thenReturn(createCM2Profile());

        //Call method
        List<String> contentModels = ts.getContentModelsInCollection("doms:Object");

        //Check calls
        verify(mockWebResource).queryParam("query",
                                           "* <info:fedora/fedora-system:def/model#hasModel> <info:fedora/fedora-system:ContentModel-3.0>");
        verify(mockWebResource).post(String.class);

        verify(mockFedora).getObjectProfile("doms:CM1", null);
        verify(mockFedora).getObjectProfile("doms:CM2", null);
        verifyNoMoreInteractions(mockFedora, mockWebResource);

        //Check result.
        assertEquals("Should return the one object that is part of the collection.",
                     Collections.singletonList("doms:CM1"), contentModels);

    }

    /**
     * Test generic query.
     *
     * @throws Exception
     */
    @Test
    public void testGenericQuery() throws Exception {
        //Fixture: Return one known relation
        when(mockWebResource.post(String.class))
                .thenReturn("info:fedora/doms:Subject doms:whatsUp info:fedora/doms:Object");

        //Call method
        List<FedoraRelation> inverseRelations = ts.genericQuery("* * *");

        //Check result -
        verify(mockWebResource).queryParam("query", "* * *");
        verify(mockWebResource).post(String.class);
        verifyNoMoreInteractions(mockFedora, mockWebResource);

        assertEquals(1, inverseRelations.size());
        assertEquals("Should return the expected relation", Collections.singletonList(
                new FedoraRelation("info:fedora/doms:Subject", "doms:whatsUp", "info:fedora/doms:Object")),
                     inverseRelations);
    }

    /**
     * Profile for an object that is part of the collection.
     *
     * @return That profile.
     */
    private ObjectProfile createCM1Profile() {
        ObjectProfile result = new ObjectProfile();
        result.setPid("doms:CM1");
        result.setType(ObjectType.CONTENT_MODEL);
        result.setState("A");

        ArrayList<FedoraRelation> relations = new ArrayList<FedoraRelation>();
        relations.add(new FedoraRelation("info:fedora/doms:CM1",
                                         "http://doms.statsbiblioteket.dk/relations/default/0/1/#isPartOfCollection",
                                         "info:fedora/doms:Object"));
        result.setRelations(relations);

        return result;
    }

    /**
     * Profile for an object that is not part of the collection.
     *
     * @return That profile.
     */
    private ObjectProfile createCM2Profile() {
        ObjectProfile result = new ObjectProfile();
        result.setPid("doms:CM2");
        result.setType(ObjectType.CONTENT_MODEL);
        result.setState("A");

        result.setRelations(new ArrayList<FedoraRelation>());

        return result;
    }

}