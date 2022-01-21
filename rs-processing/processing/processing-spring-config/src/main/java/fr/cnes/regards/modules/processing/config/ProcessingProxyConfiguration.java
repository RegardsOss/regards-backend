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

import com.google.common.base.Strings;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.stream.Stream;

/**
 * This class is the configuration for proxy.
 *
 * @author gandrieu
 */
@Configuration
public class ProcessingProxyConfiguration {

    @Bean public Proxy proxy(HttpProxyProperties config) {
        Proxy proxy = Strings.isNullOrEmpty(config.getHost())
                ? Proxy.NO_PROXY
                : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getHost(), Option.of(config.getPort()).getOrElse(80)));
        return proxy;
    }

    @Bean("nonProxyHosts") public Set<String> nonProxyHosts(HttpProxyProperties config) {
        return Stream
            .of(Option.of(config.getNoproxy())
                .getOrElse(() -> "")
                .split(",")
            )
            .filter(s -> !s.isEmpty())
            .map(String::trim)
            .collect(HashSet.collector());
    }
}

