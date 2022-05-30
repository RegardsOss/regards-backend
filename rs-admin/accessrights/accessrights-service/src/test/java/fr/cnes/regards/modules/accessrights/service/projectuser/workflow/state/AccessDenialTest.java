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
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnDenyEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessDenialTest extends UserAccessUpdateTest {

    @Test
    public void fail_if_not_waiting_access() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        Assertions.assertThatExceptionOfType(EntityTransitionForbiddenException.class)
                  .isThrownBy(() -> userWorkflowManager.denyAccess(givenUser));
    }

    @Test
    public void update_user_access_to_unauthorized() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().waitingAccess().build();
        userWorkflowManager.denyAccess(givenUser);
        assertThat(updatedUser().getStatus()).isEqualTo(UserStatus.ACCESS_DENIED);
    }

    @Test
    public void publish_user_access_update() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().waitingAccess().build();
        userWorkflowManager.denyAccess(givenUser);
        assertThat(publishedEvent()).isNotNull();
        assertThat(publishedEvent()).isInstanceOf(OnDenyEvent.class);
    }
}
