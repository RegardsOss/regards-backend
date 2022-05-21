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

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LicenseAcceptTest extends LicenseServiceTest {

    @Test
    public void license_is_accepted() throws Exception {
        givenProject = LicenseTestFactory.aProjectWithLicence();
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        licenseService = givenLicenseService();

        LicenseDTO license = licenseService.acceptLicense();
        assertThat(license.isAccepted()).isTrue();
    }

    @Test
    public void user_is_updated_in_database() throws Exception {
        givenProject = LicenseTestFactory.aProjectWithLicence();
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        licenseService = givenLicenseService();

        licenseService.acceptLicense();
        verify(userService, times(1)).updateUser(givenUser.getId(), givenUser);
    }

    @Test
    public void fail_if_unable_to_retrieve_project() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aMissingProject();
        licenseService = givenLicenseService();

        assertThatExceptionOfType(EntityException.class) //
                                                         .isThrownBy(() -> licenseService.acceptLicense());
    }

    @Test
    public void notify_license_acceptation() throws Exception {
        givenUser = LicenseTestFactory.anExploitWithoutLicense();
        givenProject = LicenseTestFactory.aProjectWithLicence();
        licenseService = givenLicenseService();

        licenseService.acceptLicense();

        ArgumentCaptor<LicenseEvent> eventCaptor = ArgumentCaptor.forClass(LicenseEvent.class);
        verify(publisher).publish(eventCaptor.capture());

        LicenseEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo(LicenseAction.ACCEPT);
        assertThat(event.getUser()).isEqualTo(givenUser.getEmail());
        assertThat(event.getLicenseLink()).isEqualTo(LicenseTestFactory.LINK_TO_LICENSE);
    }
}
