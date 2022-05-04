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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.domain.download.ValidDownload;
import fr.cnes.regards.modules.search.rest.download.LicenseAcceptationStatus;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AcceptLicenseAndDownloadTest {

    private final FakeProductFactory productFactory;

    private final FakeFileFactory fileFactory;

    public AcceptLicenseAndDownloadTest() {
        productFactory = new FakeProductFactory();
        fileFactory = new FakeFileFactory();
    }

    @Test
    public void fail_if_license_acceptation_failed() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseAcceptationStatus.FAILURE);
        assertThatExceptionOfType(ModuleException.class).isThrownBy(() -> downloader.acceptLicenseAndDownloadFile(
                productFactory.authorizedProduct().toString(),
                fileFactory.validFile()))
            // Error Message contains http status from licence acceptation (mocked)
            .withMessageContaining("" + HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void send_error_response_if_http_error_occurred_during_licence_acceptation() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseAcceptationStatus.HTTP_ERROR);
        ResponseEntity<Download> response = downloader.acceptLicenseAndDownloadFile(productFactory.authorizedProduct()
                                                                                        .toString(),
                                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void accept_license_and_then_download() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseAcceptationStatus.ACCEPTED);
        ResponseEntity<Download> response = downloader.acceptLicenseAndDownloadFile(productFactory.authorizedProduct()
                                                                                        .toString(),
                                                                                    fileFactory.validFile());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isInstanceOf(ValidDownload.class);
    }
}
