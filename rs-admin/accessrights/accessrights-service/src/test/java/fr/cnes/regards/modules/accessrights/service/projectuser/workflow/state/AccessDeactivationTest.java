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
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnInactiveEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Fache
 **/
public class AccessDeactivationTest extends UserAccessUpdateTest {

    @Test
    public void fail_if_not_authorized() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().waitingActivation().build();
        Assertions.assertThatExceptionOfType(EntityTransitionForbiddenException.class)
            .isThrownBy(() -> userWorkflowManager.inactiveAccess(givenUser));
    }

    @Test
    public void update_user_access_to_inactive() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.inactiveAccess(givenUser);
        assertThat(updatedUser().getStatus()).isEqualTo(UserStatus.ACCESS_INACTIVE);
    }

    @Test
    public void publish_user_access_update() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        userWorkflowManager.inactiveAccess(givenUser);
        assertThat(publishedEvent()).isNotNull();
        assertThat(publishedEvent()).isInstanceOf(OnInactiveEvent.class);
    }
}
