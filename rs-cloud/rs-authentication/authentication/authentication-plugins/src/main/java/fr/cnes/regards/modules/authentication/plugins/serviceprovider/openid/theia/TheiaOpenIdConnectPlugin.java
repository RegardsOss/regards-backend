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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectPlugin;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdUserInfoResponse;

@Plugin(
    id = TheiaOpenIdConnectPlugin.ID,
    author = "REGARDS Team",
    description = "Plugin handling the authentication via OpenId Service Provider",
    version = TheiaOpenIdConnectPlugin.VERSION,
    contact = "regards@c-s.fr",
    license = "GPLv3",
    owner = "CNES",
    url = "https://regardsoss.github.io/"
)
public class TheiaOpenIdConnectPlugin extends OpenIdConnectPlugin<TheiaOpenIdUserInfoResponse> {

    public static final String ID = "TheiaOpenId";

    public static final String VERSION = "1.0";

    @Override
    protected Class<TheiaOpenIdConnectClient> getOauth2ClientType() {
        return TheiaOpenIdConnectClient.class;
    }
}
