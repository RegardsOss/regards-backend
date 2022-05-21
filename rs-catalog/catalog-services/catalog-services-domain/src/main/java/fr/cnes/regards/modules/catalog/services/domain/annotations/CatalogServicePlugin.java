/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.domain.annotations;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

import java.lang.annotation.*;

/**
 * Indicates that an annotated class is a "CatalogServicePlugin", that it is to say a Plugin from rs-catalog which is applied to entities.
 *
 * <p>It must be used in conjunction with @Plugin annotation otherwise it will be unused.
 *
 * @author Xavier-Alexandre Brochard
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CatalogServicePlugin {

    /**
     * The possible application modes of the annotated type.
     */
    ServiceScope[] applicationModes();

    /**
     * The entity types to which the annotated type is meant to be applied.
     */
    EntityType[] entityTypes();

}
