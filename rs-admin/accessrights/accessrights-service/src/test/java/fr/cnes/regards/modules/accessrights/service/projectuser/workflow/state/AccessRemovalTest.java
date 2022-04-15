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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.ProjectUserEvent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Thomas Fache
 **/
public class AccessRemovalTest extends UserAccessUpdateTest {

    @Test
    public void remove_user_from_tenant() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.removeAccess(givenUser);
        verify(userAccessor).deleteById(givenUser.getId());
    }

    @Test
    public void remove_tenant_in_account() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.removeAccess(givenUser);
        verify(accountAccessor).unlink(givenUser.getEmail(), TENANT);
    }

    @Test
    public void remove_pending_mail_verification() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.removeAccess(givenUser);
        verify(mailVerifier).deleteTokenForProjectUser(givenUser);
    }

    @Test
    public void publish_user_deletion_from_tenant() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.removeAccess(givenUser);
        assertThat(publishedRemovalEvent().getAction()).isEqualTo(ProjectUserAction.DELETE);
        assertThat(publishedRemovalEvent().getEmail()).isEqualTo(givenUser.getEmail());
    }

    private ProjectUserEvent publishedRemovalEvent() {
        ArgumentCaptor<ProjectUserEvent> eventCaptor = ArgumentCaptor.forClass(ProjectUserEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        ProjectUserEvent event = eventCaptor.getValue();
        return event;
    }
}
