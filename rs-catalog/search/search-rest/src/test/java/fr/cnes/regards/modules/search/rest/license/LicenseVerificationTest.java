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
package fr.cnes.regards.modules.search.rest.license;

import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.rest.CatalogDownloadTester;
import fr.cnes.regards.modules.search.rest.FakeFileFactory;
import fr.cnes.regards.modules.search.rest.FakeProductFactory;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseVerificationTest {

    private final FakeProductFactory productFactory;

    private final FakeFileFactory fileFactory;

    public LicenseVerificationTest() {
        productFactory = new FakeProductFactory();
        fileFactory = new FakeFileFactory();
    }

    @Test
    public void dont_verify_license_for_quicklook_sd() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.quicklook_sd().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void dont_verify_license_for_quicklook_md() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.quicklook_md().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void dont_verify_license_for_quicklook_hd() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.quicklook_hd().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void dont_verify_license_for_thumbnail() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.thumbnail().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void verify_license_for_description() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.description().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void verify_license_for_rawdata() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.rawdata().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    public void verify_license_for_aip() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.aip().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    public void verify_license_for_other() throws Exception {
        CatalogDownloadTester downloader = new CatalogDownloadTester(LicenseVerificationStatus.NOT_ACCEPTED);
        ResponseEntity<Download> response = downloader.downloadFile(productFactory.authorizedProduct().toString(),
                                                                    fileFactory.other().getChecksum());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

}
