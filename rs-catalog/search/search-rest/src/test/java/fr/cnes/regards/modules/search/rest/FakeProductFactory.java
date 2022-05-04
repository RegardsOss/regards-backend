/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

import java.util.UUID;

public class FakeProductFactory {

    public static final UUID VALID_URN_ID = UUID.fromString("b4cf92ae-d2dd-3ec4-9a5e-7bd7ff1f4234");

    private static final UUID UNKNOWN_URN_ID = UUID.randomUUID();

    private static final UUID UNAUTHORIZED_URN_ID = UUID.randomUUID();

    public String invalidProduct() {
        return "INVALID_URN";
    }

    public UniformResourceName unknownProduct() {
        return UniformResourceName.build("ID", EntityType.DATA, "TENANT", UNKNOWN_URN_ID, 1);
    }

    public UniformResourceName unauthorizedProduct() {
        return UniformResourceName.build("ID", EntityType.DATA, "TENANT", UNAUTHORIZED_URN_ID, 1);
    }

    public UniformResourceName authorizedProduct() {
        return validProduct();
    }

    public UniformResourceName validProduct() {
        return UniformResourceName.build("AIP", EntityType.DATA, "project1", VALID_URN_ID, 1);
    }

}
