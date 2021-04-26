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
package fr.cnes.regards.framework.security.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.header.Header;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 *  Force cache control in response header. Using CacheControl method from spring builder is not possible due to webSecurityAutoConfiguration
 *  enable setShouldWriteHeadersEagerly to prevent issue from spring security with headers NPE.<br/>
 *  https://github.com/spring-projects/spring-security/issues/9175<br/>
 *  <br/>
 *  This writer disable cache control for endpoints : <ul>
 *  <li> /downloads/* : For file download from catalog microservice</li>
 *  </ul>
 *
 *
 *
 *  Overrides {@link CacheControlHeadersWriter} from spring security
 *
 *  @author Sébastien Binda
 */
public final class CustomCacheControlHeadersWriter implements HeaderWriter {

    public static final String EXPIRES = "Expires";

    public static final String PRAGMA = "Pragma";

    public static final String CACHE_CONTROL = "Cache-Control";

    private final HeaderWriter delegateCache;

    private final HeaderWriter delegateNoCache;

    /**
     * Creates a new instance
     */
    public CustomCacheControlHeadersWriter() {
        this.delegateNoCache = new StaticHeadersWriter(createNoCacheHeaders());
        this.delegateCache = new StaticHeadersWriter(createCacheHeaders());
    }

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (hasHeader(response, CACHE_CONTROL) || hasHeader(response, EXPIRES) || hasHeader(response, PRAGMA)
                || (response.getStatus() == HttpStatus.NOT_MODIFIED.value())) {
            return;
        }
        // Disable cache for catalog microservice download endpoint
        if (request.getRequestURI().startsWith("/downloads/")) {
            this.delegateCache.writeHeaders(request, response);
        } else {
            this.delegateNoCache.writeHeaders(request, response);
        }
    }

    private boolean hasHeader(HttpServletResponse response, String headerName) {
        return response.getHeader(headerName) != null;
    }

    private static List<Header> createNoCacheHeaders() {
        List<Header> headers = new ArrayList<>(3);
        headers.add(new Header(CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate"));
        headers.add(new Header(PRAGMA, "no-cache"));
        headers.add(new Header(EXPIRES, "0"));
        return headers;
    }

    private static List<Header> createCacheHeaders() {
        List<Header> headers = new ArrayList<>(3);
        headers.add(new Header(CACHE_CONTROL, "public, max-age=15552000"));
        return headers;
    }
}
