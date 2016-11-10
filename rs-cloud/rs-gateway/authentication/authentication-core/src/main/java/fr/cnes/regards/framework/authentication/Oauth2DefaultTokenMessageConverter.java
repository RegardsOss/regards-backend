/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

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
