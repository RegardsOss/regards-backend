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
package fr.cnes.regards.modules.notifier.service.plugin;

import org.springframework.beans.factory.annotation.Value;

import com.google.gson.JsonElement;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Fail sender to test notification resending after an error occured during the first tie
 * @author kevin
 *
 */
@Plugin(author = "REGARDS Team", description = "Fail recipient sender", id = "TestSendFail", version = "1.0.0",
        contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class RecipientSenderTestFail extends RabbitMQSender {

    // if if fail = true the send will deliberaly fail
    @Value("${notifier.test.fail:true}")
    private boolean isFail;

    @Override
    public boolean send(JsonElement element, String action) {
        if (isFail) {
            return false;
        }
        return super.send(element, action);
    }

}
