/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.entities.rest;

import com.google.common.net.HttpHeaders;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.dao.ICollectionRepository;
import fr.cnes.regards.modules.entities.dao.IDocumentRepository;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DescriptionFile;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.service.ICollectionService;
import fr.cnes.regards.modules.entities.service.IDocumentService;
import fr.cnes.regards.modules.models.dao.IModelRepository;
import fr.cnes.regards.modules.models.domain.Model;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lmieulet
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
@ContextConfiguration(classes = { ControllerITConfig.class })
public class DocumentControllerIT extends AbstractRegardsTransactionalIT {

    /**
     *
     */
    private static final String DOCUMENTS_DOCUMENT_ID = "/documents/{document_id}";

    /**
     *
     */
    private static final String DOCUMENTS = "/documents";

    /**
     *
     */
    private static final String DOCUMENTS_DOCUMENT_ID_ASSOCIATE = DOCUMENTS_DOCUMENT_ID + "/associate";

    private static final String DOCUMENTS_DOCUMENT_ID_DISSOCIATE = DOCUMENTS_DOCUMENT_ID + "/dissociate";

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocumentControllerIT.class);

    private Model model1;

    private Model model2;

    private Collection collection1;

    private Collection collection2;

    private Document document1;

    private Document document2;

    private Document document3;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private ICollectionRepository collectionRepository;

    @Autowired
    private IModelRepository modelRepository;

    @Autowired
    private GsonBuilder gsonBuilder;

    private List<ResultMatcher> expectations;

    @Autowired
    private IDocumentService documentService;

    @Before
    public void initRepos() {
        expectations = new ArrayList<>();
        // Bootstrap default values
        model1 = Model.build("modelName1", "model desc", EntityType.COLLECTION);
        model2 = Model.build("modelName2", "model desc", EntityType.DOCUMENT);
        model2 = modelRepository.save(model2);
        model1 = modelRepository.save(model1);

        collection1 = new Collection(model1, "PROJECT", "collection1");
        collection1.setSipId("SipId1");
        collection1.setLabel("label");
        collection1.setCreationDate(OffsetDateTime.now());

        collection2 = new Collection(model1, "PROJECT", "collection2");
        collection2.setSipId("SipId1");
        collection2.setLabel("label");
        collection2.setCreationDate(OffsetDateTime.now());

        document1 = new Document(model2, "PROJECT", "document1");
        document1.setSipId("SipId2");
        document1.setLabel("label");
        document1.setCreationDate(OffsetDateTime.now());

        document2 = new Document(model2, "PROJECT", "document2");
        document2.setSipId("SipId3");
        document2.setLabel("label");
        document2.setCreationDate(OffsetDateTime.now());
        final Set<String> doc2Tags = new HashSet<>();
        doc2Tags.add(collection1.getIpId().toString());
        document2.setTags(doc2Tags);

        collection1 = collectionRepository.save(collection1);
        collection2 = collectionRepository.save(collection2);
        document1 = documentRepository.save(document1);
        document2 = documentRepository.save(document2);
    }



    @Requirement("REGARDS_DSL_DAM_COL_510")
    @Purpose("Shall retrieve all documents")
    @Test
    public void testGetAllDocuments() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        performDefaultGet(DOCUMENTS, expectations, "Failed to fetch document list");
    }



    @Requirement("REGARDS_DSL_DAM_DOC_010")
    @Purpose("Shall create a new document")
    @Test
    public void testPostDocument() {
        final Document document3 = new Document(model1, null, "document3");
        document3.setSipId("SipId3");
        document3.setLabel("label");
        document3.setCreationDate(OffsetDateTime.now());

        expectations.add(MockMvcResultMatchers.status().isCreated());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_ID, Matchers.notNullValue()));

        final String collectionStr = gsonBuilder.create().toJson(document3);
        performDefaultPost(DOCUMENTS, document3, expectations, "Failed to create a new document");
    }


    @Test
    public void testGetDocumentById() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        performDefaultGet(DOCUMENTS_DOCUMENT_ID, expectations,
                "Failed to fetch a specific document using its id", document1.getId());
    }


    @Requirement("REGARDS_DSL_DAM_DOC_110")
    @Purpose("Shall delete the document")
    @Test
    public void testDeleteDocument() {
        expectations.add(MockMvcResultMatchers.status().isNoContent());
        performDefaultDelete(DOCUMENTS_DOCUMENT_ID, expectations,
                "Failed to delete a specific document using its id", document1.getId());
    }


    @Requirement("REGARDS_DSL_DAM_DOC_210")
    @Test
    public void testUpdateDocument() {
        final Document documentClone = new Document(document1.getModel(), "", "document1clone");
        documentClone.setIpId(document1.getIpId());
        documentClone.setCreationDate(document1.getCreationDate());
        documentClone.setId(document1.getId());
        documentClone.setTags(document1.getTags());
        documentClone.setSipId(document1.getSipId() + "new");
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        performDefaultPut(DOCUMENTS_DOCUMENT_ID, documentClone, expectations,
                "Failed to update a specific document using its id", document1.getId());
    }



    @Requirement("REGARDS_DSL_DAM_DOC_230")
    @Purpose("Shall dissociate tag from the document")
    @Test
    public void testDissociateDocuments() {
        final List<UniformResourceName> toDissociate = new ArrayList<>();
        toDissociate.add(collection1.getIpId());
        expectations.add(MockMvcResultMatchers.status().isNoContent());
        performDefaultPut(DOCUMENTS_DOCUMENT_ID_DISSOCIATE, toDissociate, expectations,
                "Failed to dissociate collections from one collection using its id", document2.getId());
    }


    @Requirement("REGARDS_DSL_DAM_DOC_230")
    @Purpose("Shall associate tag to the document")
    @Test
    public void testAssociateDocuments() {
        final List<UniformResourceName> toAssociate = new ArrayList<>();
        toAssociate.add(collection2.getIpId());
        expectations.add(MockMvcResultMatchers.status().isNoContent());
        performDefaultPut(DOCUMENTS_DOCUMENT_ID_ASSOCIATE, toAssociate, expectations,
                "Failed to associate collections from one collection using its id", document1.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
