/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners;

import fr.cnes.regards.modules.accessrights.service.config.AccessRightsTemplateConfiguration;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events.OnDenyEvent;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailService;
import fr.cnes.regards.modules.accessrights.service.utils.AccessRightsEmailWrapper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Listen to {@link OnDenyEvent} in order to warn the user its account request was refused.
 *
 * @author Xavier-Alexandre Brochard
 */
@Profile("!nomail")
@Component
public class SendProjectUserDeniedEmailListener implements ApplicationListener<OnDenyEvent> {

    private final AccessRightsEmailService accessRightsEmailService;

    public SendProjectUserDeniedEmailListener(AccessRightsEmailService accessRightsEmailService) {
        this.accessRightsEmailService = accessRightsEmailService;
    }

    @Override
    public void onApplicationEvent(OnDenyEvent event) {
        AccessRightsEmailWrapper wrapper = new AccessRightsEmailWrapper()
                .setProjectUser(event.getProjectUser())
                .setSubject("[REGARDS] User access denied")
                .setTo(Collections.singleton(event.getProjectUser().getEmail()))
                .setTemplate(AccessRightsTemplateConfiguration.USER_DENIED_TEMPLATE_NAME)
                .setDefaultMessage("Your access request was refused by admin.");

        accessRightsEmailService.sendEmail(wrapper);
    }
}