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
package fr.cnes.regards.framework.gson.strategy;

import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Exclusion strategy for PagedModel object used in REST controllers
 *
 * @author Iliana Ghazali
 **/
public class PagedModelExclusionStrategy implements ExclusionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagedModelExclusionStrategy.class);

    @Override
    public boolean shouldSkipField(FieldAttributes pFieldAttributes) {
        List<String> ignoredAttributes = Lists.newArrayList("fallbackType", "fullType");
        final boolean isSkipped = ignoredAttributes.contains(pFieldAttributes.getName());
        if (isSkipped) {
            LOGGER.debug("Skipping field {} in class {}.", pFieldAttributes.getName(), pFieldAttributes.getClass());
        }
        return isSkipped;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
