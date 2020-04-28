/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.autoconfigure;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.gson.Gson;

/**
 * @author sbinda
 *
 */
public class GsonHttpMessageConverterCustom extends GsonHttpMessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonHttpMessageConverterCustom.class);

    private static final String PRETTY_PRINT_PARAMETER = "_pretty";

    private Gson prettyGson;

    @Override
    public Gson getGson() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String prettyValues = request.getParameter(PRETTY_PRINT_PARAMETER);
        if ((prettyValues != null)) {
            LOGGER.trace("pretty print enabled.");
            return this.prettyGson;
        } else {
            return super.getGson();
        }
    }

    public void setPrettyGson(Gson gson) {
        prettyGson = gson;
    }

}
