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
package fr.cnes.regards.modules.notifier.domain.plugin;

import org.dom4j.rule.Rule;

import com.google.gson.JsonElement;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * Describe action to applied to a {@link Rule}
 * @author Kevin Marchois
 *
 */
@FunctionalInterface
@PluginInterface(description = "Element rule matcher")
public interface IRuleMatcher {

    /**
     * Verify if a {@link JsonElement} match with a rule
     * @param element {@link JsonElement} to verify if it matches
     * @return true if match, false otherwise
     */
    boolean match(JsonElement element);
}
