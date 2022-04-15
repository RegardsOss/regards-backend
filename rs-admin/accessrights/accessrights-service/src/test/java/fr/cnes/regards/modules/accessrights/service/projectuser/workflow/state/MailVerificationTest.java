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

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Fache
 **/
public class MailVerificationTest extends UserAccessUpdateTest {

    @Test
    public void fail_if_user_is_not_waiting_mail_verification() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().authorized().build();
        EmailVerificationToken emailToken = new EmailVerificationToken(givenUser, "", "");

        Assertions.assertThatExceptionOfType(EntityTransitionForbiddenException.class)
            .isThrownBy(() -> userWorkflowManager.verifyEmail(emailToken));
    }

    @Test
    public void fail_if_verification_delay_expired() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().waitingMailVerification().build();
        EmailVerificationToken emailToken = new EmailVerificationToken(givenUser, "", "");
        emailToken.setExpiryDate(LocalDateTime.now().minusHours(1));

        Assertions.assertThatExceptionOfType(EntityOperationForbiddenException.class)
            .isThrownBy(() -> userWorkflowManager.verifyEmail(emailToken));
    }

    @Test
    public void update_user_access_to_authorized() throws Exception {
        ProjectUser givenUser = UserBuilder.aUser().waitingMailVerification().build();
        EmailVerificationToken emailToken = new EmailVerificationToken(givenUser, "", "");

        userWorkflowManager.verifyEmail(emailToken);

        assertThat(updatedUser().getStatus()).isEqualTo(UserStatus.ACCESS_GRANTED);
    }

}
