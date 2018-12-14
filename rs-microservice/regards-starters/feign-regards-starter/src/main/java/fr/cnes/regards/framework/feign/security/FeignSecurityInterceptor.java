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
package fr.cnes.regards.framework.feign.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import fr.cnes.regards.framework.security.utils.HttpConstants;

/**
 * This class inject token in the {@link RequestTemplate}
 * @author Marc Sordi
 */
public class FeignSecurityInterceptor implements RequestInterceptor {

    /**
     * Feign security manager
     */
    protected final FeignSecurityManager feignSecurityManager;

    public FeignSecurityInterceptor(FeignSecurityManager feignSecurityManager) {
        this.feignSecurityManager = feignSecurityManager;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + feignSecurityManager.getToken());
    }

}
