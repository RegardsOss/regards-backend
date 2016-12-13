/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.autoconfigure;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Class SwaggerMessageConverter
 *
 * Http Message Converter specific for springfox Swagger. Springfox is working with Jackson. As we use Gson in the
 * microservice-core we have to define here a specific converter.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 *
 */
public class SwaggerMessageConverter extends MappingJackson2HttpMessageConverter {

    @Override
    public boolean canWrite(final Class<?> pClazz, final MediaType pMedia) {
        boolean result = false;
        // Only user Jackson for Oauth2 Token.
        if (pClazz.getName().contains("springfox")) {
            result = super.canWrite(pClazz, pMedia);
        }
        return result;
    }

}
