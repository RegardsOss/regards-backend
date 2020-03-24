/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.feign;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.utils.HttpConstants;

/**
 * Class TokenClientProvider
 *
 * Feign client token provider. Add the JWT Token from the security context to the client requests.
 * @author CS
 */
public class TokenClientProvider<T> implements Target<T> {

    /**
     * Feign client server url.
     */
    private final String url;

    /**
     * Feign client interface
     */
    private final Class<T> clazz;

    /**
     * Feign security manager
     */
    private final FeignSecurityManager feignSecurityManager;

    public TokenClientProvider(final Class<T> pClass, final String pUrl, FeignSecurityManager pFeignSecurityManager) {
        url = pUrl;
        clazz = pClass;
        this.feignSecurityManager = pFeignSecurityManager;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Request apply(final RequestTemplate pTemplate) {
        if (pTemplate.url().indexOf("http") != 0) {
            pTemplate.insert(0, url);
        }
        // Apply security
        pTemplate.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + feignSecurityManager.getToken());
        return pTemplate.request();
    }

    @Override
    public String name() {
        return "resources";
    }

    @Override
    public Class<T> type() {
        return clazz;
    }

    @Override
    public String url() {
        return url;
    }

}
