/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.rest.entities;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.rest.entities.dto.DataFileReference;
import fr.cnes.regards.modules.dam.service.entities.ICollectionService;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test entity attachments processing
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=attachment",
                                   "regards.dam.local_storage.path=target/store",
                                   "regards.dam.attachment.inputs.path=/" })
@MultitenantTransactional
public class AttachmentControllerIT extends AbstractRegardsTransactionalIT {

    private static final Path ATTACHMENT_FOLDER = Paths.get("src", "test", "resources", "attachments");

    private static final String PDF_CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE
                                                   + " ;charset="
                                                   + StandardCharsets.UTF_8;

    private static final String HTML_CONTENT_TYPE = MediaType.TEXT_HTML_VALUE + " ;charset=" + StandardCharsets.UTF_8;

    @Autowired
    private IModelService modelService;

    @Autowired
    private ICollectionService collectionService;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private AttachmentController attachmentController;

    private Collection collection;

    private Dataset dataset;

    @Before
    public void init() throws ModuleException {

        // Create a collection model
        Model collectionModel = Model.build("MODEL", "Empty model for testing", EntityType.COLLECTION);
        modelService.createModel(collectionModel);

        // Create a collection
        collection = new Collection(collectionModel, getDefaultTenant(), "COL1", "Collection label");
        collectionService.create(collection);
        Project defaultProjectMock = new Project("defaultTenantMock", "", true, getDefaultTenant());
        defaultProjectMock.setHost("http://localhost");
        Mockito.when(projectsClient.retrieveProject(getDefaultTenant()))
               .thenReturn(ResponseEntity.ok(EntityModel.of(defaultProjectMock)));

        dataset = new Dataset(collectionModel, getDefaultTenant(), "DS1", "Dataset label");
        datasetService.create(dataset);
    }

    private MockMultipartFile getMultipartFile(String originalFilename, String contentType) throws IOException {
        return getMultipartFile(originalFilename, originalFilename, contentType);
    }

    private MockMultipartFile getMultipartFile(String filename, String originalFilename, String contentType)
        throws IOException {
        Path filePath = ATTACHMENT_FOLDER.resolve(filename);
        return new MockMultipartFile("file", originalFilename, contentType, Files.newInputStream(filePath));
    }

    private MockMultipartFile getMultipartFileRefs(DataFileReference... refs) {
        Assert.assertNotNull(refs);
        String contentAsString = gson(Arrays.asList(refs));
        return new MockMultipartFile("refs", "", MediaType.APPLICATION_JSON_VALUE, contentAsString.getBytes());
    }

    @Test
    public void attachDescription() throws IOException {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expect(MockMvcResultMatchers.jsonPath(
                                                              "$.content.feature.files."
                                                              + DataType.DESCRIPTION
                                                              + ".length()",
                                                              Matchers.equalTo(2)));

        List<MockMultipartFile> files = new ArrayList<>();
        files.add(getMultipartFile("description.pdf", PDF_CONTENT_TYPE));
        files.add(getMultipartFile("description2.pdf", PDF_CONTENT_TYPE));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DESCRIPTION);
    }

    @Test
    public void attachUrlDescription() {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expect(MockMvcResultMatchers.jsonPath(
                                                              "$.content.feature.files."
                                                              + DataType.DESCRIPTION
                                                              + ".length()",
                                                              Matchers.equalTo(1)));

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(HTML_CONTENT_TYPE));
        ref.setUri("https://tools.ietf.org/html/rfc7946");
        ref.setFilename("rfc7946");

        files.add(getMultipartFileRefs(ref));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DESCRIPTION);
    }

    @Test
    public void attachGetAndRemoveDescription() {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expect(MockMvcResultMatchers.jsonPath(
                                                              "$.content.feature.files."
                                                              + DataType.DESCRIPTION
                                                              + ".length()",
                                                              Matchers.equalTo(1)));

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(PDF_CONTENT_TYPE));
        ref.setUri("https://public.ccsds.org/pubs/650x0m2.pdf");
        ref.setFilename("650x0m2.pdf");

        files.add(getMultipartFileRefs(ref));

        ResultActions result = performDefaultFileUpload(AttachmentController.TYPE_MAPPING
                                                        + AttachmentController.ATTACHMENTS_MAPPING,
                                                        files,
                                                        customizer,
                                                        "Attachment error",
                                                        collection.getIpId().toString(),
                                                        DataType.DESCRIPTION);

        String json = payload(result);
        String checksum = JsonPath.read(json, "$.content.feature.files." + DataType.DESCRIPTION + "[0].checksum");
        Assert.assertNotNull(checksum);

        // Get it
        customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        performDefaultGet(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENT_MAPPING,
                          customizer,
                          "Download error",
                          collection.getIpId().toString(),
                          checksum);

        // Remove it
        customizer = customizer().expectStatusOk();

        performDefaultDelete(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENT_MAPPING,
                             customizer,
                             "Remove error",
                             collection.getIpId().toString(),
                             checksum);
        Assert.assertThat("Description must be removed",
                          JsonPathMatchers.hasNoJsonPath("$.content.feature.files." + DataType.DESCRIPTION));

    }

    @Test
    public void attachRefAndNormalDescription() throws IOException {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expect(MockMvcResultMatchers.jsonPath(
                                                              "$.content.feature.files."
                                                              + DataType.DESCRIPTION
                                                              + ".length()",
                                                              Matchers.equalTo(2)));

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(PDF_CONTENT_TYPE));
        ref.setUri("https://public.ccsds.org/pubs/650x0m2.pdf");
        ref.setFilename("650x0m2.pdf");
        files.add(getMultipartFileRefs(ref));

        // Create normal description
        files.add(getMultipartFile("description.pdf", PDF_CONTENT_TYPE));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DESCRIPTION);
    }

    @Test
    public void attachDescriptionWithoutName() throws IOException {
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        String pdfContentType = MediaType.APPLICATION_PDF_VALUE + " ;charset=" + StandardCharsets.UTF_8;

        List<MockMultipartFile> files = new ArrayList<>();
        files.add(getMultipartFile("description.pdf", "", pdfContentType));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DESCRIPTION);
    }

    @Test
    public void attachDescriptionWithBadContentType() throws IOException {
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        String pdfContentType = MediaType.APPLICATION_ATOM_XML_VALUE + " ;charset=" + StandardCharsets.UTF_8;

        List<MockMultipartFile> files = new ArrayList<>();
        files.add(getMultipartFile("description.pdf", pdfContentType));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DESCRIPTION);
    }

    private void uploadDocument() throws IOException {
        // Upload document
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expect(MockMvcResultMatchers.jsonPath(
                                                              "$.content.feature.files."
                                                              + DataType.DOCUMENT
                                                              + ".length()",
                                                              Matchers.equalTo(1)));

        List<MockMultipartFile> files = new ArrayList<>();
        files.add(getMultipartFile("description.docx", MediaType.APPLICATION_OCTET_STREAM_VALUE));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 collection.getIpId().toString(),
                                 DataType.DOCUMENT);
    }

    @Test
    public void test_errorWhenNotAuthorizedProtocol() throws IOException {
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(PDF_CONTENT_TYPE));
        ref.setUri("ftp://public.ccsds.org/pubs/650x0m2.pdf");
        ref.setFilename("650x0m2.pdf");

        // Create normal description
        files.add(getMultipartFileRefs(ref));

        ResultActions attachmentError = performDefaultFileUpload(AttachmentController.TYPE_MAPPING
                                                                 + AttachmentController.ATTACHMENTS_MAPPING,
                                                                 files,
                                                                 customizer,
                                                                 "Attachment error",
                                                                 dataset.getIpId().toString(),
                                                                 DataType.RAWDATA);
        MockHttpServletResponse response = attachmentError.andReturn().getResponse();
        Assertions.assertTrue(response.getContentAsString().contains("Forbidden URL protocol"));
    }

    @Test
    public void test_wrongInternalFile() throws UnsupportedEncodingException {
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(PDF_CONTENT_TYPE));
        ref.setUri("file:/path-totally-random.zzz");
        ref.setFilename("what.what");

        // Create normal description
        files.add(getMultipartFileRefs(ref));

        ResultActions attachmentError = performDefaultFileUpload(AttachmentController.TYPE_MAPPING
                                                                 + AttachmentController.ATTACHMENTS_MAPPING,
                                                                 files,
                                                                 customizer,
                                                                 "Attachment error",
                                                                 dataset.getIpId().toString(),
                                                                 DataType.RAWDATA);
        MockHttpServletResponse response = attachmentError.andReturn().getResponse();
        Assertions.assertTrue(response.getContentAsString().contains("not exists"));
    }

    @Test
    public void test_successInternalFile() {
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.OK);

        List<MockMultipartFile> files = new ArrayList<>();

        // Create description reference
        DataFileReference ref = new DataFileReference();
        ref.setMimeType(MediaType.parseMediaType(PDF_CONTENT_TYPE));
        ref.setUri(AttachmentControllerIT.class.getClassLoader().getResource("dataModel.xml").toString());
        ref.setFilename("dataMod.pdf");

        // Create normal description
        files.add(getMultipartFileRefs(ref));

        performDefaultFileUpload(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENTS_MAPPING,
                                 files,
                                 customizer,
                                 "Attachment error",
                                 dataset.getIpId().toString(),
                                 DataType.RAWDATA);
    }

    @Test
    public void attachDocument() throws IOException {

        uploadDocument();

        // Download document
        DataFile dataFile = collection.getFiles().get(DataType.DOCUMENT).stream().findFirst().get();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        performDefaultGet(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENT_MAPPING,
                          customizer,
                          "Download error",
                          collection.getIpId().toString(),
                          dataFile.getChecksum());
    }

    @Test
    public void removeDocument() throws IOException {

        uploadDocument();

        // Remove document
        DataFile dataFile = collection.getFiles().get(DataType.DOCUMENT).stream().findFirst().get();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        performDefaultDelete(AttachmentController.TYPE_MAPPING + AttachmentController.ATTACHMENT_MAPPING,
                             customizer,
                             "Remove error",
                             collection.getIpId().toString(),
                             dataFile.getChecksum());
    }

}
