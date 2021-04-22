/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import org.dom4j.rule.Rule;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import sun.security.pkcs11.Secmod;

/**
 * Describe action to applied to a {@link Rule}
 * @author Kevin Marchois
 *
 */
@FunctionalInterface
@PluginInterface(description = "Element rule matcher")
public interface IRuleMatcher {

    /**
     * Verify if a {@link JsonObject} match with a rule
     * @param jsonObject {@link JsonObject} to verify if it matches
     * @return true if match, false otherwise
     */
    boolean match(JsonObject jsonObject);
}
