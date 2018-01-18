/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * This class allows to inject public authentication header dynamically.
 *
 * @author Marc Sordi
 *
 */
public class CustomHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * Custom dynamic header
     */
    private final Map<String, String> customHeaders;

    public CustomHttpServletRequest(HttpServletRequest request) {
        super(request);
        customHeaders = new HashMap<>();
    }

    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // Look up in custom headers
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        } else {
            return super.getHeader(name);
        }
    }

}
