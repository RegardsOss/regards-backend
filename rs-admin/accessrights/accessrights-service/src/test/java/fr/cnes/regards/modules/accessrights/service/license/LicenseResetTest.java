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

import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LicenseResetTest extends LicenseServiceTest {

    @Test
    public void delegate_license_reset_to_user_service() throws Exception {
        licenseService = givenLicenseService();

        licenseService.resetLicence();

        verify(userService, times(1)).resetLicence();
    }

    @Test
    public void notify_license_acceptation() throws Exception {
        licenseService = givenLicenseService();

        licenseService.resetLicence();

        ArgumentCaptor<LicenseEvent> eventCaptor = ArgumentCaptor.forClass(LicenseEvent.class);
        verify(publisher).publish(eventCaptor.capture());

        LicenseEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo(LicenseAction.RESET);
        assertThat(event.getUser()).isEmpty();
        assertThat(event.getLicenseLink()).isEmpty();
    }
}
