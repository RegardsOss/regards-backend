/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.download;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.search.rest.FakeFileFactory;
import fr.cnes.regards.modules.search.rest.FakeProductFactory;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@TestPropertySource(locations = { "classpath:test.properties" },
                    properties = { "regards.tenant=opensearch",
                        "spring.jpa.properties.hibernate.default_schema=opensearch" })
public class CatalogDownloadControllerIT extends AbstractRegardsTransactionalIT {

    private static final String DOWNLOAD_AIP_FILE = "/downloads/{aip_id}/files/{checksum}";

    private static final String ERROR_MESSAGE_ON_MVN_ERROR = "Error while executing mvn request";

    private static final Logger LOG = LoggerFactory.getLogger(CatalogDownloadControllerIT.class);

    private final FakeFileFactory fileFactory;

    private final FakeProductFactory productFactory;

    @Autowired
    protected IEsRepository esRepository;

    @Autowired
    private LicenseAccessor licenseAccessor;

    @Autowired
    private ILicenseClient licenseClient;

    @Autowired
    private IStorageRestClient storageClient;

    @Autowired
    private ICatalogSearchService searchService;

    public CatalogDownloadControllerIT() {
        fileFactory = new FakeFileFactory();
        productFactory = new FakeProductFactory();
    }

    // Remarque : Cette classe de TI sert à tester l'intégration Spring
    // On vérifie que la fonctionnalité s'intègre correctement dans le contexte Spring
    // notamment que les liens sont correctement positionnés dans le cas d'une license non acceptée

    @Before
    public void prepareData() throws ModuleException, InterruptedException {
        initIndex(getDefaultTenant());
        ((CatalogSearchServiceMock) searchService).mockGet();
    }

    protected void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Test
    public void response_is_forbidden_if_user_cant_access_to_product() {
        setupLicenseAccessor(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED);
        setupStorageDownload(StorageDownloadStatus.NOMINAL);
        downloadAndVerifyRequest(productFactory.unauthorizedProduct(),
                                 fileFactory.validFile(),
                                 customizer().expectStatusForbidden());
    }

    @Test
    public void locked_response_if_license_is_not_accepted() {
        setupLicenseAccessor(LicenseVerificationStatus.NOT_ACCEPTED, LicenseAcceptationStatus.NOT_CALLED);
        setupStorageDownload(StorageDownloadStatus.NOMINAL);

        RequestBuilderCustomizer requestVerifications = customizer().expectStatus(HttpStatus.LOCKED)
                                                                    .expectValue("license",
                                                                                 LicenseClientMock.LINK_TO_LICENCE);

        ResultActions mvcResult = downloadAndVerifyRequest(productFactory.validProduct(),
                                                           fileFactory.validFile(),
                                                           requestVerifications);
        verifyLinkToAcceptLicense(mvcResult);
    }

    private void verifyLinkToAcceptLicense(ResultActions mvcResult) {
        String requestUrl = mvcResult.andReturn().getRequest().getRequestURL().toString();
        String expectedLink = requestUrl + "?acceptLicense=true";
        try {
            mvcResult.andExpect(MockMvcResultMatchers.jsonPath("accept").value(expectedLink));
        } catch (Exception e) {
            Assert.fail("Unable to verify link to accept license and download" + e.getMessage());
        }
    }

    @Test
    public void file_not_found_response_if_storage_does_not_find_the_file() {
        setupLicenseAccessor(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED);
        setupStorageDownload(StorageDownloadStatus.FAILURE);
        downloadAndVerifyRequest(productFactory.validProduct(),
                                 fileFactory.validFile(),
                                 customizer().expectStatusNotFound());
    }

    @Test
    public void internal_error_response_if_an_unexpected_error_occurred() {
        setupLicenseAccessor(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED);
        setupStorageDownload(StorageDownloadStatus.HTTP_ERROR);
        downloadAndVerifyRequest(productFactory.validProduct(),
                                 fileFactory.validFile(),
                                 customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void ok_response_if_file_is_downloaded() {
        setupLicenseAccessor(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED);
        setupStorageDownload(StorageDownloadStatus.NOMINAL);
        downloadAndVerifyRequest(productFactory.validProduct(), fileFactory.validFile(), customizer().expectStatusOk());
    }

    @Test
    public void ok_response_if_license_is_not_accepted_but_download_with_acceptation() {
        setupLicenseAccessor(LicenseVerificationStatus.NOT_ACCEPTED, LicenseAcceptationStatus.ACCEPTED);
        setupStorageDownload(StorageDownloadStatus.NOMINAL);
        RequestBuilderCustomizer expectations = customizer().expectStatusOk().addParameter("acceptLicense", "true");
        downloadAndVerifyRequest(productFactory.validProduct(), fileFactory.validFile(), expectations);
    }

    private void setupLicenseAccessor(LicenseVerificationStatus verificationStatus,
                                      LicenseAcceptationStatus acceptationStatus) {
        // Clean cache before each test to keep test independent.
        // Otherwise, test can fail because of a bad cached license state.
        licenseAccessor.cleanCache();
        ((LicenseClientMock) licenseClient).setup(verificationStatus, acceptationStatus);

    }

    private void setupStorageDownload(StorageDownloadStatus downloadStatus) {
        ((IStorageRestClientMock) storageClient).setup(downloadStatus);
    }

    private ResultActions downloadAndVerifyRequest(UniformResourceName product,
                                                   String file,
                                                   RequestBuilderCustomizer requestVerifications) {
        return performDefaultGet(DOWNLOAD_AIP_FILE, requestVerifications, ERROR_MESSAGE_ON_MVN_ERROR, product, file);
    }
}
