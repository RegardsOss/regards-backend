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

import fr.cnes.regards.modules.search.rest.download.LicenseAcceptationStatus;
import fr.cnes.regards.modules.search.rest.download.LicenseVerificationStatus;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseVerificationTests {

    private final FakeProductFactory productFactory;

    private ResponseEntity<Void> response;

    public LicenseVerificationTests() {
        productFactory = new FakeProductFactory();
    }

    @Test
    public void send_not_found_response_if_no_product_is_found() throws Exception {
        CatalogDownloadTester controller = new CatalogDownloadTester();
        response = controller.testProductAccess(productFactory.unknownProduct());
        assertResponseIsEmptyWithStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    public void send_forbidden_response_if_product_access_is_unauthorized() throws Exception {
        CatalogDownloadTester controller = new CatalogDownloadTester();
        response = controller.testProductAccess(productFactory.unauthorizedProduct());
        assertResponseIsEmptyWithStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    public void send_internal_error_response_if_license_check_failed() throws Exception {
        CatalogDownloadTester controller = new CatalogDownloadTester(LicenseVerificationStatus.HTTP_ERROR);
        response = controller.testProductAccess(productFactory.authorizedProduct());
        assertResponseIsEmptyWithStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void send_locked_response_if_license_is_not_accepted() throws Exception {
        CatalogDownloadTester controller = new CatalogDownloadTester(LicenseAcceptationStatus.ACCEPTED);
        response = controller.testProductAccess(productFactory.authorizedProduct());
        assertResponseIsEmptyWithStatus(HttpStatus.LOCKED);
    }

    @Test
    public void send_ok_response_if_license_is_accepted_and_file_accessible() throws Exception {
        CatalogDownloadTester controller = new CatalogDownloadTester();
        response = controller.testProductAccess(productFactory.authorizedProduct());
        assertResponseIsEmptyWithStatus(HttpStatus.OK);
    }

    private void assertResponseIsEmptyWithStatus(HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders()).isEmpty();
        assertThat(response.hasBody()).isFalse();
    }
}
