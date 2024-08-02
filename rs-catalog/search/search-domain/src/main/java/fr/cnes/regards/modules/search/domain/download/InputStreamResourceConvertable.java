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
package fr.cnes.regards.modules.search.domain.download;

import feign.Response;
import fr.cnes.regards.framework.feign.ResponseStreamProxy;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;

/**
 * Extends the {@link InputStreamResource} in order to override {@link InputStreamResourceConvertable#contentLength()}
 * Store and expose the content length of the stream
 *
 * @author Léo Mieulet
 */
public abstract class InputStreamResourceConvertable extends InputStreamResource {

    private final Integer contentLength;

    /**
     * @param response response to proxy
     */
    public InputStreamResourceConvertable(Response response) throws IOException {
        super(new ResponseStreamProxy(response));
        this.contentLength = response.body().length();
    }

    /**
     * Override the default content length method, which reads the {@link InputStreamResource#inputStream} to deduce its length.
     * Spring does not allow InputStream to be read several times, so this class retrieve the length from the proxied request
     *
     * @return content length
     * @see https://github.com/spring-projects/spring-framework/issues/24522
     */
    @Override
    public long contentLength() {
        return contentLength;
    }
}
