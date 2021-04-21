/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

import static feign.Util.ensureClosed;

/**
 * This class is a looging decoder used only in tests.
 *
 * @author gandrieu
 */
public class GsonLoggingDecoder implements Decoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonLoggingDecoder.class);

    private final Gson gson;

    public GsonLoggingDecoder(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        try {
            if (response.body() == null) {
                return null;
            }
            Reader reader = response.body().asReader();
            String content = IOUtils.toString(reader);
            Request request = response.request();
            LOGGER.info("{} {}\n>>>\n{}\n<<<", request.httpMethod().name(), request.url(), content);
            try {
                return gson.fromJson(content, type);
            } catch (JsonIOException e) {
                if ((e.getCause() != null) && (e.getCause() instanceof IOException)) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            } finally {
                ensureClosed(reader);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new JsonParseException("wups", e);
        }
    }
}