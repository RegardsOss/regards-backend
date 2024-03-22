/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.notifier.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;

import java.util.Collection;

/**
 * @author tguillou
 */
@Plugin(author = "REGARDS Team",
        description = "Recipient sender 2",
        id = PluginWithSensitiveParam.PLUGIN_ID,
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class PluginWithSensitiveParam implements IRecipientNotifier {

    public static final String PLUGIN_ID = "pluginWithSensitiveParam";

    public static final String SENSITIVE_PARAM_NAME = "sensitiveParam";

    @PluginParameter(name = SENSITIVE_PARAM_NAME, label = "Sensitive param", defaultValue = "false", sensitive = true)
    public String sensitiveInfo;

    public String getSensitiveInfo() {
        return sensitiveInfo;
    }

    @Override
    public Collection<NotificationRequest> send(Collection<NotificationRequest> requestsToSend) {
        return null;
    }

    @Override
    public String getRecipientLabel() {
        return null;
    }

    @Override
    public boolean isAckRequired() {
        return false;
    }
}
