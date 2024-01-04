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
package fr.cnes.regards.modules.search.service.license;

import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Service
public class LicenseClientMock implements ILicenseClient {

    public static final String LINK_TO_LICENCE = "link/to/licence";

    public final ILicenseClient licenseClient;

    public LicenseClientMock() {
        licenseClient = mock(ILicenseClient.class);
    }

    public void setup(LicenseVerificationStatus verificationStatus, LicenseAcceptationStatus acceptationStatus) {
        mockRetrieve(verificationStatus);
        mockAcceptation(acceptationStatus);
    }

    private void mockRetrieve(LicenseVerificationStatus verificationStatus) {
        if (verificationStatus == LicenseVerificationStatus.HTTP_ERROR) {
            HttpServerErrorException httpError = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
                                                                              "some http error");
            when(licenseClient.retrieveLicense()).thenThrow(httpError);
        } else {
            // False only if user is user without license
            boolean isAccepted = verificationStatus == LicenseVerificationStatus.ACCEPTED;
            int status = verificationStatus == LicenseVerificationStatus.FAILURE ?
                HttpStatus.SERVICE_UNAVAILABLE.value() :
                HttpStatus.OK.value();
            when(licenseClient.retrieveLicense()).thenReturn(fakeLicense(isAccepted, status));
        }
    }

    private void mockAcceptation(LicenseAcceptationStatus acceptationStatus) {
        if (acceptationStatus == LicenseAcceptationStatus.HTTP_ERROR) {
            when(licenseClient.acceptLicense()).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY,
                                                                                       "http error on acceptation"));
        } else {
            int status = acceptationStatus == LicenseAcceptationStatus.FAILURE ?
                HttpStatus.NO_CONTENT.value() :
                HttpStatus.OK.value();
            when(licenseClient.acceptLicense()).thenReturn(fakeLicense(true, status));
        }
    }

    private ResponseEntity<EntityModel<LicenseDTO>> fakeLicense(boolean isAccepted, int status) {
        LicenseDTO licence = new LicenseDTO(isAccepted, LINK_TO_LICENCE);
        return ResponseEntity.status(status).headers(new HttpHeaders()).body(EntityModel.of(licence));
    }

    @Override
    public ResponseEntity<EntityModel<LicenseDTO>> retrieveLicense() {
        return licenseClient.retrieveLicense();
    }

    @Override
    public ResponseEntity<EntityModel<LicenseDTO>> acceptLicense() {
        return licenseClient.acceptLicense();
    }

    @Override
    public ResponseEntity<Void> resetLicense() {
        throw new NotImplementedException("resetLicense is not mocked yet");
    }

}