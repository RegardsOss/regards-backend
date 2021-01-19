/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.domain.plugin;

import java.util.Collection;
import java.util.Collections;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent10;

/**
 * @author kevin
 *
 */
@Plugin(author = "REGARDS Team", description = "Fail recipient sender", id = "fail", version = "1.0.0",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class RecipientSenderFail extends AbstractRecipientSender<NotificationEvent10> {

    /**
     * Yes this is only a public static and not final attribute. It allows tests to alter the logic without recreating a plugin configuration
     */
    public static boolean RECIPIENT_FAIL = false;

    @Override
    public Collection<NotificationRequest> send(Collection<NotificationRequest> requestsToSend) {
        if (RECIPIENT_FAIL) {
            return requestsToSend;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    NotificationEvent10 buildEvent(JsonElement element, JsonElement action) {
        return null;
    }
}
