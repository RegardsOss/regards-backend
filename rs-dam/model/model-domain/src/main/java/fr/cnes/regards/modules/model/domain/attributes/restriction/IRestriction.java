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
package fr.cnes.regards.modules.model.domain.attributes.restriction;

import fr.cnes.regards.modules.model.domain.IXmlisable;
import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 * Restriction interface
 *
 * @author msordi
 */
public interface IRestriction extends IXmlisable<Restriction> {

    RestrictionType getType();

    /**
     * Check if this restriction supports the given {@link PropertyType}
     */
    Boolean supports(PropertyType pAttributeType);

    /**
     * Check if the current restriction is valid. If no specific implementation all Restriction are valid.
     */
    default Boolean validate() {
        return Boolean.TRUE;
    }
}
