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
package fr.cnes.regards.modules.notifier.domain.plugin;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent6;

/**
 * @author kevin
 */
@Plugin(author = "REGARDS Team", description = "Recipient sender 6", id = RecipientSender6.PLUGIN_ID, version = "1.0.0",
    contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class RecipientSender6 extends AbstractRecipientSender<NotificationEvent6> {

    public static final String PLUGIN_ID = "RecipientSender6";

    @Override
    NotificationEvent6 buildEvent(JsonElement element, JsonElement action) {
        return NotificationEvent6.build(element, action);
    }
}
