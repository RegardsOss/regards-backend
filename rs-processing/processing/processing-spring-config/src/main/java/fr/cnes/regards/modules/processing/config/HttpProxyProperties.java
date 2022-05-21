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
package fr.cnes.regards.modules.processing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * This class reads http proxy configuration.
 *
 * @author gandrieu
 */
@Configuration
public class HttpProxyProperties {

    private final String host;

    private final Integer port;

    private final String noproxy;

    public HttpProxyProperties(@Value("${http.proxy.host:#{null}}") String host,
                               @Value("${http.proxy.port:#{null}}") Integer port,
                               @Value("${http.proxy.noproxy:#{null}}") String noproxy) {
        this.host = host;
        this.port = port;
        this.noproxy = noproxy;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getNoproxy() {
        return noproxy;
    }
}
