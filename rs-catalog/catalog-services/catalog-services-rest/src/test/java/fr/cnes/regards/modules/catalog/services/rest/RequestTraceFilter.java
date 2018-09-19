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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.trace.TraceProperties;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.stereotype.Component;

/**
 * Workaround for {@link CatalogServicesControllerIT}. Some tests fails randomely during trace of request response.
 * This class ensure to trace response headers only if there is some.
 * @author Sébastien Binda
 */
@Component
public class RequestTraceFilter extends WebRequestTraceFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestTraceFilter.class);

    RequestTraceFilter(TraceRepository repository, TraceProperties properties) {
        super(repository, properties);
    }

    @Override
    protected void enhanceTrace(Map<String, Object> trace, HttpServletResponse response) {
        LOG.info("response {}", response.getStatus());
        if (response.getHeaderNames() != null) {
            LOG.info("response {}", response.getHeaderNames().size());
            super.enhanceTrace(trace, response);
        } else {
            LOG.error("ERROR --------------------> Unable to trace request response headers !!!!!!");
        }
    }
}