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
package fr.cnes.regards.modules.notifier.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;

/**
 * Describe action for Recipient
 * @author Kevin Marchois
 *
 */
@FunctionalInterface
@PluginInterface(description = "Recipient sender plugin")
public interface IRecipientNotifier {

    /**
     *
     * @param toSend {@link NotificationRequest} to send
     * @return true if succed, false otherwise
     */
    //FIXME see if we can send multiple notification at same time
    boolean send(NotificationRequest toSend);

}
