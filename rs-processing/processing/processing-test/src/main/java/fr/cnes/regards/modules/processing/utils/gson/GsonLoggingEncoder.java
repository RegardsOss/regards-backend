/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.Gson;
import feign.RequestTemplate;
import feign.codec.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * This class is a logging encoder used only in tests.
 *
 * @author gandrieu
 */
public class GsonLoggingEncoder implements Encoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonLoggingEncoder.class);

    private final Gson gson;

    public GsonLoggingEncoder(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        String bodyText = gson.toJson(object, bodyType);
        LOGGER.info("Encoding object {}\n>>>\n{}\n<<<", object, bodyText);
        template.body(bodyText);
    }

}
