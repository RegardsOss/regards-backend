/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.adapters.sample8;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 * @author Marc Sordi
 */
@Gsonable
public abstract class AbstractEntitySample {

    /**
     * Collection name
     */
    public static final String NAME = "entity";

    /**
     * name to identify entity in test
     */
    private String name = NAME;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

}
