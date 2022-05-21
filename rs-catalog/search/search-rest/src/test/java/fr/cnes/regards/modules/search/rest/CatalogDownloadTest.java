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

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.autoconfigure.CustomCacheControlHeadersWriter;
import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.domain.download.FailedDownload;
import fr.cnes.regards.modules.search.domain.download.MissingLicenseDownload;
import fr.cnes.regards.modules.search.domain.download.ValidDownload;
import fr.cnes.regards.modules.search.rest.download.LicenseVerificationStatus;
import fr.cnes.regards.modules.search.rest.download.StorageDownloadStatus;
import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CatalogDownloadTest {

    private final FakeProductFactory productFactory;

    private final FakeFileFactory fileFactory;

    public CatalogDownloadTest() {
        productFactory = new FakeProductFactory();
        fileFactory = new FakeFileFactory();
    }

    @Test
    public void fail_if_product_is_invalid() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        assertThatIllegalArgumentException().isThrownBy(() -> downloader.downloadFile(productFactory.invalidProduct(),
                                                                                      fileFactory.validFile()));
    }

    @Test
    public void fail_if_product_is_not_found() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> downloader.downloadFile(productFactory.unknownProduct()
                                                                                                                        .toString(),
                                                                                                          fileFactory.validFile()));
    }

    @Test
    public void validate_user_privileges() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.unauthorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void validate_licence_acceptation() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isInstanceOf(MissingLicenseDownload.class);
        MissingLicenseDownload body = (MissingLicenseDownload) response.getBody();
        assertThat(body.getLinkToLicense()).isEqualTo("link/to/licence");
        assertThat(body.getLinkToAcceptAndDownload()).isEqualTo(linkToAcceptAndDownloadFile());
    }

    private String linkToAcceptAndDownloadFile() {
        return "/downloads/" + productFactory.authorizedProduct() + "/files/" + fileFactory.validFile() + "?"
            + "isContentInline=" + true + "&" + "acceptLicense=true";
    }

    @Test
    public void send_error_response_if_licence_verification_failed() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.FAILURE);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void send_error_response_if_http_error_occurred_during_licence_verification() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.HTTP_ERROR);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void send_error_response_if_http_error_occurred_during_download() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(StorageDownloadStatus.HTTP_ERROR);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void fail_if_file_is_not_readable() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        assertThatIOException().isThrownBy(() -> downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                         fileFactory.invalidFile()));
    }

    @Test
    public void add_storage_error_stream_in_response() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(StorageDownloadStatus.FAILURE);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isInstanceOf(FailedDownload.class);
        String fileContent = readFile((FailedDownload) response.getBody());
        assertThat(fileContent).isEqualTo("errors.");
    }

    @Test
    public void add_file_retrieved_by_storage_in_response() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(StorageDownloadStatus.NOMINAL);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isInstanceOf(ValidDownload.class);
        String fileContent = readFile((ValidDownload) response.getBody());
        assertThat(fileContent).isEqualTo("content");
    }

    @Test
    public void does_not_add_storage_headers_in_response() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.validFile());
        assertThat(response.getHeaders()).isEmpty();
    }

    @Test
    public void add_storage_headers_in_servlet_response() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        downloader.downloadFile(productFactory.authorizedProduct().toString(), fileFactory.validFile());
        HttpServletResponse servletResponse = downloader.servletResponse;
        verify(servletResponse, times(1)).setHeader("key1", "value1");
        verify(servletResponse, times(1)).setHeader("key1", "value2");
    }

    @Test
    public void filter_cache_storage_headers_in_servlet_response() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester();
        downloader.downloadFile(productFactory.authorizedProduct().toString(), fileFactory.validFile());
        HttpServletResponse servletResponse = downloader.servletResponse;

        verify(servletResponse, never()).setHeader(CustomCacheControlHeadersWriter.CACHE_CONTROL, "cache");
        verify(servletResponse, never()).setHeader(CustomCacheControlHeadersWriter.EXPIRES, "expires");
        verify(servletResponse, never()).setHeader(CustomCacheControlHeadersWriter.PRAGMA, "pragma");
    }

    private String readFile(InputStreamResource downloadedFile) throws IOException {
        byte[] content = new byte[7];
        downloadedFile.getInputStream().read(content);
        return new String(content);
    }
}
