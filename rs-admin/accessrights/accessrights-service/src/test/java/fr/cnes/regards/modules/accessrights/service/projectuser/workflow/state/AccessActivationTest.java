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

import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnActiveEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessActivationTest extends UserAccessUpdateTest {

    @Test
    public void fail_is_not_inactive() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        Assertions.assertThatExceptionOfType(EntityTransitionForbiddenException.class)
                  .isThrownBy(() -> userWorkflowManager.activeAccess(givenUser));
    }

    @Test
    public void update_user_access_to_authorized() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().inactive().build();
        userWorkflowManager.activeAccess(givenUser);
        assertThat(updatedUser().getStatus()).isEqualTo(UserStatus.ACCESS_GRANTED);
    }

    @Test
    public void publish_user_access_update() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().inactive().build();
        userWorkflowManager.activeAccess(givenUser);
        ApplicationEvent event = publishedEvent();
        assertThat(event).isNotNull();
        assertThat(event).isInstanceOf(OnActiveEvent.class);
    }
}
