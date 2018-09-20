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
package fr.cnes.regards.modules.catalog.services.rest;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.trace.TraceProperties;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Workaround for {@link CatalogServicesControllerIT}. Some tests fails randomely during trace of request response.
 * This class ensure to trace response headers only if there is some.
 * @author Sébastien Binda
 */
@Component
public class RequestTraceFilter extends WebRequestTraceFilter {

    RequestTraceFilter(TraceRepository repository, TraceProperties properties) {
        super(repository, properties);
    }

    @Override
    protected void enhanceTrace(Map<String, Object> trace, HttpServletResponse response) {
        if (!response.getContentType().equals(MediaType.IMAGE_PNG_VALUE)
                && !response.getContentType().equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            super.enhanceTrace(trace, response);
        }
    }
}