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
package fr.cnes.regards.modules.featureprovider.domain.plugin;

import java.lang.reflect.Type;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

@Service
public class FactoryParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(FactoryParameters.class);

    @Autowired
    private Gson gson;

    @SuppressWarnings("unchecked")
    public <T> T getParameter(JsonObject parameters, String memberName, Type type) throws ModuleException {
        JsonElement element = parameters.get(memberName);
        checkNotNull(element, memberName);
        Object parameter = null;
        try {
            parameter = gson.fromJson(element, type);
        } catch (Exception ex) {
            badConversion(element, memberName, ex);
        }
        return (T) parameter;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptionalParameter(JsonObject parameters, String memberName, Type type)
            throws ModuleException {
        JsonElement element = parameters.get(memberName);
        if (element == null) {
            return Optional.empty();
        }
        Object parameter = null;
        try {
            parameter = gson.fromJson(element, type);
        } catch (Exception ex) {
            badConversion(element, memberName, ex);
        }
        return Optional.of((T) parameter);
    }

    private void checkNotNull(JsonElement element, String path) throws ModuleException {
        if (element == null) {
            String errorMessage = String.format("Missing parameter %s", path);
            LOGGER.error(errorMessage);
            throw new ModuleException(errorMessage);
        }
    }

    private void badConversion(JsonElement element, String path, Exception ex) throws ModuleException {
        String errorMessage = String.format("Bad conversion for parameter %s : %s", path, ex.getMessage());
        LOGGER.error(errorMessage, ex);
        throw new ModuleException(errorMessage);
    }
}
