/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais;

import javax.validation.constraints.NotNull;

/**
 *
 * OAIS semantic
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class Semantic {

    @NotNull
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof Semantic) && description.equals(((Semantic) pOther).description);
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }
}
