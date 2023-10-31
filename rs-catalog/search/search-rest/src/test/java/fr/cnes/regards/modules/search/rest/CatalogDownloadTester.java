/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.authentication.autoconfigure.AuthenticationAutoConfiguration;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.rest.download.*;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogDownloadTester {

    private static final boolean ACCEPT_LICENSE = true;

    private static final boolean DONT_ACCEPT_LICENSE = false;

    private static final Boolean CONTENT_IN_LINE = true;

    public final HttpServletResponse servletResponse;

    private final ILicenseClient licenseClient;

    private final CatalogDownloadController controller;

    public CatalogDownloadTester(LicenseVerificationStatus verificationStatus,
                                 LicenseAcceptationStatus acceptationStatus,
                                 StorageDownloadStatus downloadStatus) {

        controller = new CatalogDownloadController();
        ReflectionTestUtils.setField(controller, "authResolver", mockUser());
        ReflectionTestUtils.setField(controller, "runtimeTenantResolver", mockTenant());
        ReflectionTestUtils.setField(controller, "searchService", mockAccessVerification());
        ReflectionTestUtils.setField(controller, "businessSearchService", mockBusinessSearchService());
        licenseClient = mockLicenseClient(verificationStatus, acceptationStatus);
        ReflectionTestUtils.setField(controller, "licenseAccessor", mockLicenseAccesses());
        ReflectionTestUtils.setField(controller, "storageRestClient", mockFileDownload(downloadStatus));

        servletResponse = mock(HttpServletResponse.class);
    }

    public CatalogDownloadTester(LicenseAcceptationStatus acceptationStatus) {
        this(LicenseVerificationStatus.NOT_ACCEPTED, acceptationStatus, StorageDownloadStatus.NOMINAL);
    }

    public CatalogDownloadTester(LicenseVerificationStatus verificationStatus) {
        this(verificationStatus, LicenseAcceptationStatus.NOT_CALLED, StorageDownloadStatus.NOMINAL);
    }

    public CatalogDownloadTester(StorageDownloadStatus downloadStatus) {
        this(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED, downloadStatus);
    }

    public CatalogDownloadTester() {
        this(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.NOT_CALLED, StorageDownloadStatus.NOMINAL);
    }

    private IAuthenticationResolver mockUser() {
        return new AuthenticationAutoConfiguration().defaultAuthenticationResolver();
    }

    private IRuntimeTenantResolver mockTenant() {
        IRuntimeTenantResolver tenantResolver = mock(IRuntimeTenantResolver.class);
        when(tenantResolver.getTenant()).thenReturn("A_PROJECT");
        return tenantResolver;
    }

    private ICatalogSearchService mockAccessVerification() {
        // On ne mocke que la vérification d'accès.
        // On n'a pas besoin d'initialiser complétement le service
        // qui a plusieurs responsabilités.
        IAccessRightFilter accessRightFilterMock = Mockito.mock(IAccessRightFilter.class);
        try {
            Mockito.when(accessRightFilterMock.getUserAccessGroups()).thenReturn(null);
        } catch (AccessRightFilterException e) {
            throw new RuntimeException(e);
        }
        CatalogSearchServiceMock searchService = new CatalogSearchServiceMock(null, accessRightFilterMock, null, null);
        searchService.mockGet();
        return searchService;
    }

    private IBusinessSearchService mockBusinessSearchService() {
        IBusinessSearchService businessSearchServiceMock = Mockito.mock(IBusinessSearchService.class);
        try {
            when(businessSearchServiceMock.isContentAccessGranted(Mockito.any())).thenReturn(true);
        } catch (AccessRightFilterException e) {
            throw new RuntimeException(e);
        }
        return businessSearchServiceMock;
    }

    private LicenseAccessor mockLicenseAccesses() {
        ISubscriber subscriber = mock(ISubscriber.class);
        LicenseAccessor licenseAccessor = new LicenseAccessor(licenseClient, subscriber);
        return licenseAccessor;
    }

    private ILicenseClient mockLicenseClient(LicenseVerificationStatus verificationStatus,
                                             LicenseAcceptationStatus acceptationStatus) {
        LicenseClientMock licenseClient = new LicenseClientMock();
        licenseClient.setup(verificationStatus, acceptationStatus);
        return licenseClient;
    }

    private IStorageRestClient mockFileDownload(StorageDownloadStatus downloadStatus) {
        IStorageRestClientMock storageClient = new IStorageRestClientMock();
        storageClient.setup(downloadStatus);
        return storageClient;
    }

    public ResponseEntity<Download> downloadFile(String productUrn, String fileChecksum)
        throws ModuleException, IOException {
        return downloadFile(productUrn, fileChecksum, DONT_ACCEPT_LICENSE);
    }

    public ResponseEntity<Download> acceptLicenseAndDownloadFile(String productUrn, String fileChecksum)
        throws ModuleException, IOException {
        return downloadFile(productUrn, fileChecksum, ACCEPT_LICENSE);
    }

    private ResponseEntity<Download> downloadFile(String productUrn, String fileChecksum, boolean acceptLicense)
        throws ModuleException, IOException {
        return controller.downloadFile(productUrn, fileChecksum, CONTENT_IN_LINE, acceptLicense, servletResponse);
    }

    public ResponseEntity<Void> testProductAccess(UniformResourceName productUrn, String fileChecksum) {
        return controller.testProductAccess(productUrn.toString(), fileChecksum);
    }
}