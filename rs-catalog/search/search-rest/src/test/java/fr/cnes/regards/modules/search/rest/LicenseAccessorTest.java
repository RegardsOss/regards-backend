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
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import fr.cnes.regards.modules.search.rest.download.LicenseAcceptationStatus;
import fr.cnes.regards.modules.search.rest.download.LicenseAccessor;
import fr.cnes.regards.modules.search.rest.download.LicenseClientMock;
import fr.cnes.regards.modules.search.rest.download.LicenseVerificationStatus;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LicenseAccessorTest {

    private static final String A_USER = "A_USER";

    private static final String OTHER_USER = "OTHER_USER";

    private static final String A_PROJECT = "A_PROJECT";

    private static final String OTHER_PROJECT = "OTHER_PROJECT";

    private final LicenseAccessor licenseAccessor;

    private final LicenseClientMock licenseClient;

    private final LicenseAccessor.LicenseEventHandler eventHandler;

    public LicenseAccessorTest() {
        licenseClient = new LicenseClientMock();
        licenseClient.setup(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        ISubscriber subscriber = mock(ISubscriber.class);
        ArgumentCaptor<LicenseAccessor.LicenseEventHandler> handlerCaptor = ArgumentCaptor.forClass(LicenseAccessor.LicenseEventHandler.class);

        licenseAccessor = new LicenseAccessor(licenseClient, subscriber);
        verify(subscriber, times(1)).subscribeTo(eq(LicenseEvent.class), handlerCaptor.capture());

        eventHandler = handlerCaptor.getValue();
    }

    @Test
    public void keep_license_state_in_cache_when_retrieve_license() throws Exception {
        licenseClient.setup(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        licenseAccessor.retrieveLicense(A_USER, A_PROJECT);

        verify(licenseClient.licenseClient, times(1)).retrieveLicense();
    }

    @Test
    public void cache_key_is_based_on_user_and_tenant() throws Exception {
        licenseClient.setup(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        licenseAccessor.retrieveLicense(OTHER_USER, A_PROJECT);
        licenseAccessor.retrieveLicense(A_USER, OTHER_PROJECT);

        verify(licenseClient.licenseClient, times(3)).retrieveLicense();
    }

    @Test
    public void update_cache_when_accept_license() throws Exception {
        licenseClient.setup(LicenseVerificationStatus.NOT_ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        LicenseDTO license = licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        assertThat(license.isAccepted()).isFalse();

        licenseAccessor.acceptLicense(A_USER, A_PROJECT);

        LicenseDTO updatedLicense = licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        assertThat(updatedLicense.isAccepted()).isTrue();

        verify(licenseClient.licenseClient, times(1)).retrieveLicense();
        verify(licenseClient.licenseClient, times(1)).acceptLicense();
    }

    @Test
    public void update_cache_when_receive_license_acceptation_notification() throws Exception {
        licenseClient.setup(LicenseVerificationStatus.NOT_ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        LicenseDTO license = licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        assertThat(license.isAccepted()).isFalse();

        eventHandler.handle(A_PROJECT, aLicenseAcceptationEvent(A_USER));

        // Cache is updated with the accepted license
        LicenseDTO updatedLicense = licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        assertThat(updatedLicense.isAccepted()).isTrue();
    }

    private LicenseEvent aLicenseAcceptationEvent(String forUser) {
        return new LicenseEvent(LicenseAction.ACCEPT, forUser, LicenseClientMock.LINK_TO_LICENCE);
    }

    @Test
    public void update_cache_when_receive_license_reset_notification() throws Exception {
        licenseClient.setup(LicenseVerificationStatus.ACCEPTED, LicenseAcceptationStatus.ACCEPTED);

        licenseAccessor.retrieveLicense(A_USER, A_PROJECT);
        eventHandler.handle(A_PROJECT, aLicenseResetEvent());
        licenseAccessor.retrieveLicense(A_USER, A_PROJECT);

        // Cache is invalidate after reset license event is received
        verify(licenseClient.licenseClient, times(2)).retrieveLicense();
    }

    private LicenseEvent aLicenseResetEvent() {
        return new LicenseEvent(LicenseAction.RESET, "", "");
    }
}
