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
package fr.cnes.regards.framework.authentication.internal;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

/**
 * Class Oauth2DefaultTokenMessageConverter
 *
 * Http Message Converter specific for Oauth2Token. Oauth2Token are working with Jackson. As we use Gson in the
 * microservice-core we have to define here a specific converter.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 *
 */
public class Oauth2DefaultTokenMessageConverter extends MappingJackson2HttpMessageConverter {

    @Override
    public boolean canWrite(final Class<?> pClazz, final MediaType pMedia) {
        boolean result = false;
        // Only user Jackson for Oauth2 Token.
        if (DefaultOAuth2AccessToken.class.equals(pClazz)) {
            result = super.canWrite(pClazz, pMedia);
        }
        return result;
    }

}
