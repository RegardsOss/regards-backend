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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;

/**
 * Fail sender to test notification resending after an error occured during the first tie
 * @author KEvin Marchois
 *
 */
@Plugin(author = "REGARDS Team", description = "Fail recipient sender for test purporse", id = "TestSendFail",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class RecipientSenderTestFail extends RabbitMQSender implements IRecipientNotifier {

    // if if fail = true the send will deliberaly fail
    @PluginParameter(label = "If the plugin must fail", name = "fail")
    private boolean fail;

    @Override
    public boolean send(NotificationRequest toSend) {
        if (fail) {
            return false;
        }
        return super.send(toSend);
    }

}
