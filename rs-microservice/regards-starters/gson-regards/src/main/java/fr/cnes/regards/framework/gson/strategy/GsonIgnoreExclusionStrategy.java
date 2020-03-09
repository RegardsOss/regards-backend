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

package fr.cnes.regards.framework.gson.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;

/**
 * Class GsonIgnoreExclusionStrategy
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class GsonIgnoreExclusionStrategy implements ExclusionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonIgnoreExclusionStrategy.class);

    /**
     * Type to skip
     */
    private final Class<?> typeToSkip;

    public GsonIgnoreExclusionStrategy() {
        typeToSkip = null;
    }

    public GsonIgnoreExclusionStrategy(Class<?> pTypeToSkip) {
        this.typeToSkip = pTypeToSkip;
    }

    @Override
    public boolean shouldSkipClass(Class<?> pClazz) {
        return pClazz == typeToSkip;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes pFieldAttributes) {
        final boolean isSkipped = pFieldAttributes.getAnnotation(GsonIgnore.class) != null;
        if (isSkipped) {
            LOGGER.debug(String.format("Skipping field %s in class %s.", pFieldAttributes.getName(),
                                       pFieldAttributes.getClass()));
        }
        return isSkipped;
    }
}
