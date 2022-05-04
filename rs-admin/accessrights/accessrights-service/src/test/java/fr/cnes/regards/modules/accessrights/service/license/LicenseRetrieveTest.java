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
package fr.cnes.regards.modules.accessrights.service.license;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseRetrieveTest extends LicenseServiceTest {

    @Test
    public void license_is_accepted_for_instance_admin() throws Exception {
        givenUser = LicenseTestFactory.anInstanceAdmin();
        givenProject = LicenseTestFactory.aProjectWithLicence();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.retrieveLicenseState();

        assertThat(license.isAccepted()).isTrue();
        assertThat(license.getLicenceLink()).isEqualTo(LicenseTestFactory.LINK_TO_LICENSE);
    }

    @Test
    public void license_is_accepted_if_no_license() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aProjectWithoutLicense();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.retrieveLicenseState();

        assertThat(license.isAccepted()).isTrue();
        assertThat(license.getLicenceLink()).isEmpty();
    }

    @Test
    public void license_is_accepted_if_license_is_empty() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aProjectWithEmptyLicense();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.retrieveLicenseState();

        assertThat(license.isAccepted()).isTrue();
        assertThat(license.getLicenceLink()).isEmpty();
    }

    @Test
    public void license_is_refused_for_public_user_and_license_on_project() throws Exception {
        givenUser = LicenseTestFactory.aPublicUser();
        givenProject = LicenseTestFactory.aProjectWithLicence();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.retrieveLicenseState();

        assertThat(license.isAccepted()).isFalse();
        assertThat(license.getLicenceLink()).isEqualTo(LicenseTestFactory.LINK_TO_LICENSE);
    }

    @Test
    public void evaluate_license_acceptation_otherwise() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aProjectWithLicence();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.retrieveLicenseState();

        assertThat(license.isAccepted()).isFalse();
        assertThat(license.getLicenceLink()).isEqualTo(LicenseTestFactory.LINK_TO_LICENSE);
    }

    @Test
    public void fail_if_unable_to_retrieve_project() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aMissingProject();
        licenseService = givenLicenseService();

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class) //
                  .isThrownBy(() -> licenseService.retrieveLicenseState());
    }

}
