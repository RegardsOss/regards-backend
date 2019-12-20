/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.plugin;

import com.google.gson.JsonElement;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent4;

/**
 * @author kevin
 *
 */
@Plugin(author = "REGARDS Team", description = "Recipient sender 4", id = "RecipientSender4", version = "1.0.0",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class RecipientSender4 extends AbstractRecipientSender<NotificationEvent4> {

    @Override
    NotificationEvent4 buildEvent(JsonElement element, String action) {
        return NotificationEvent4.build(element, action);
    }
}
