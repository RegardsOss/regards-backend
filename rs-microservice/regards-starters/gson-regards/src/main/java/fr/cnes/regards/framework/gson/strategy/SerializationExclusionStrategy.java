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
package fr.cnes.regards.framework.gson.strategy;

import java.lang.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Annotation based exclusion strategy
 * @author Marc Sordi
 */
public class SerializationExclusionStrategy<T extends Annotation> implements ExclusionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationExclusionStrategy.class);

    /**
     * Annotation to consider for skipping serialization
     */
    private final Class<T> annotationClazz;

    public SerializationExclusionStrategy(Class<T> annotationClazz) {
        this.annotationClazz = annotationClazz;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        if (f.getAnnotation(annotationClazz) != null) {
            LOGGER.debug(String.format("Skipping field %s in class %s.", f.getName(), f.getClass()));
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
