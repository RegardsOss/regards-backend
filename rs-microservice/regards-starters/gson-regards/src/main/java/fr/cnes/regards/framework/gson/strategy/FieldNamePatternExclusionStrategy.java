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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Field exclusion strategy by name pattern
 * @author Marc Sordi *
 */
public class FieldNamePatternExclusionStrategy implements ExclusionStrategy {

    private final Pattern pattern;

    public FieldNamePatternExclusionStrategy(String regex) {
        pattern = Pattern.compile(regex);
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        Matcher m = pattern.matcher(f.getName());
        return m.matches();
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

}
