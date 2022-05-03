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
package fr.cnes.regards.modules.notifier.conf;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.rest.RecipientController;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import junit.framework.AssertionFailedError;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RecipientControllerTest {

    private static final String RECIPIENT_1 = "recipient1";

    private static final String RECIPIENT_2 = "recipient2";

    private final IRecipientService recipientService;

    private final IResourceService resourceService;

    private final RecipientController recipientController;

    private List<PluginConfiguration> allRecipients;

    private ResponseEntity<EntityModel<PluginConfiguration>> creationResponse;

    private ResponseEntity<List<EntityModel<PluginConfiguration>>> retrieveResponse;

    private ResponseEntity<EntityModel<PluginConfiguration>> updateResponse;

    private ResponseEntity<Void> deletion_response;

    public RecipientControllerTest() {
        resourceService = mockResourceService();
        recipientService = mockRecipientService();

        recipientController = new RecipientController();
        ReflectionTestUtils.setField(recipientController, "resourceService", resourceService);
        ReflectionTestUtils.setField(recipientController, "recipientService", recipientService);
    }

    private IResourceService mockResourceService() {
        IResourceService resourceService = mock(IResourceService.class);
        ArgumentCaptor<PluginConfiguration> recipient = ArgumentCaptor.forClass(PluginConfiguration.class);
        when(resourceService.toResource(recipient.capture())).thenAnswer(i -> EntityModel.of(recipient.getValue()));
        return resourceService;
    }

    private IRecipientService mockRecipientService() {
        try {
            IRecipientService recipientService = mock(IRecipientService.class);
            allRecipients = new ArrayList<>();

            ArgumentCaptor<PluginConfiguration> recipient = ArgumentCaptor.forClass(PluginConfiguration.class);
            when(recipientService.createOrUpdateRecipient(recipient.capture())).thenAnswer(i -> {
                PluginConfiguration newRecipient = recipient.getValue();
                allRecipients.removeIf(r -> r.getBusinessId().equals(newRecipient.getBusinessId()));
                allRecipients.add(newRecipient);
                return newRecipient;
            });

            when(recipientService.getRecipients()).thenAnswer(i -> new HashSet<>(allRecipients));

            ArgumentCaptor<String> businessId = ArgumentCaptor.forClass(String.class);
            doAnswer(i -> {
                allRecipients.removeIf(p -> p.getBusinessId().equals(businessId.getValue()));
                return null;
            }).when(recipientService).deleteRecipient(businessId.capture());
            return recipientService;
        } catch (ModuleException e) {
            throw new AssertionFailedError("Error while mocking Recipient Service : " + e.getMessage());
        }
    }

    private void raiseException(IRecipientService recipientService) {
        try {
            when(recipientService.createOrUpdateRecipient(any())).thenThrow(new ModuleException());
            doThrow(new ModuleException()).when(recipientService).deleteRecipient(anyString());
        } catch (ModuleException e) {
            throw new AssertionFailedError("Error while mocking Recipient Service : " + e.getMessage());
        }
    }

    @Test
    public void create_fails_if_recipient_id_is_set() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        firstRecipient.setId(1L);

        Assertions.assertThatIllegalArgumentException()
                  .isThrownBy(() -> recipientController.createRecipient(firstRecipient));
    }

    @Test
    public void create_return_null_if_service_fails() throws Exception {
        // FIXME answer properly when an error occurred
        raiseException(recipientService);
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);

        creationResponse = recipientController.createRecipient(firstRecipient);

        assertThat(creationResponse).isNull();
    }

    @Test
    public void create_nominal_return_ok_http_response() throws Exception {
        // FIXME answer properly when an error occurred
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);

        creationResponse = recipientController.createRecipient(firstRecipient);

        assertThat(creationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void create_nominal_return_created_rule() throws Exception {
        // FIXME answer properly when an error occurred
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);

        creationResponse = recipientController.createRecipient(firstRecipient);

        assertThat(creationResponse.hasBody()).isTrue();
        EntityModel<PluginConfiguration> body = creationResponse.getBody();
        assertThat(body.getContent()).isEqualTo(firstRecipient);
        // TODO Verify links
    }

    @Test
    public void getRecipients_returns_ok_http_response() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientController.createRecipient(firstRecipient);
        recipientController.createRecipient(secondRecipient);

        // TODO parameters are unused
        retrieveResponse = recipientController.getRecipients(null, null);

        assertThat(retrieveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getRecipients_returns_all_recipients() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        PluginConfiguration secondRecipient = aRecipient(RECIPIENT_2);
        recipientController.createRecipient(firstRecipient);
        recipientController.createRecipient(secondRecipient);

        retrieveResponse = recipientController.getRecipients(null, null);

        assertThat(retrieveResponse.hasBody()).isTrue();
        List<EntityModel<PluginConfiguration>> body = retrieveResponse.getBody();
        assertThat(body).hasSize(2).extracting("content").contains(firstRecipient, secondRecipient);
        // TODO Verify links
    }

    @Test
    public void update_fails_if_recipient_has_no_id() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);

        Assertions.assertThatIllegalArgumentException()
                  .isThrownBy(() -> recipientController.updateRecipient(firstRecipient));
    }

    @Test
    public void update_returns_ok_http_response() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        firstRecipient.setId(1L);

        updateResponse = recipientController.updateRecipient(firstRecipient);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void update_returns_updated_recipient() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        firstRecipient.setId(1L);

        updateResponse = recipientController.updateRecipient(firstRecipient);

        assertThat(updateResponse.hasBody()).isTrue();
        assertThat(updateResponse.getBody().getContent()).isEqualTo(firstRecipient);
    }

    @Test
    public void delete_fails_if_deletion_fails() throws Exception {
        raiseException(recipientService);

        Assertions.assertThatExceptionOfType(ModuleException.class)
                  .isThrownBy(() -> recipientController.deleteRecipient(RECIPIENT_1));
    }

    @Test
    public void delete_returns_no_content_http_response() throws Exception {
        deletion_response = recipientController.deleteRecipient(RECIPIENT_1);

        assertThat(deletion_response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void delete_ask_for_recipient_deletion() throws Exception {
        PluginConfiguration firstRecipient = aRecipient(RECIPIENT_1);
        recipientController.createRecipient(firstRecipient);

        recipientController.deleteRecipient(RECIPIENT_1);

        assertThat(recipientController.getRecipients(null, null).getBody()).isEmpty();
    }

    private PluginConfiguration aRecipient(String withName) {
        PluginConfiguration recipient = new PluginConfiguration();
        recipient.setBusinessId(withName);
        recipient.setLabel(withName);
        recipient.setVersion("1.0.0");
        recipient.setPluginId("RecipientSender3");
        return recipient;
    }

}
