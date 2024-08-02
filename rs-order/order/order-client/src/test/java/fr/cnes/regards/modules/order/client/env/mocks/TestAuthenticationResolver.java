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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.order.client.env.mocks;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;

import java.util.Set;

import static fr.cnes.regards.modules.order.client.env.utils.OrderTestConstants.USER_EMAIL;
import static fr.cnes.regards.modules.order.client.env.utils.OrderTestConstants.USER_ROLE;

/**
 * @author Iliana Ghazali
 **/
public class TestAuthenticationResolver implements IAuthenticationResolver {

    @Override
    public String getUser() {
        return USER_EMAIL;
    }

    @Override
    public String getRole() {
        return USER_ROLE;
    }

    @Override
    public String getToken() {
        return null;
    }

    @Override
    public Set<String> getAccessGroups() {
        return IAuthenticationResolver.super.getAccessGroups();
    }

}
