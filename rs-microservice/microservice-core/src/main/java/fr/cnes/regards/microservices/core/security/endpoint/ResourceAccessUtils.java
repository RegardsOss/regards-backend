/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

/**
 * Helper class for {@link ResourceAccess}
 * 
 * @author msordi
 *
 */
public class ResourceAccessUtils {

    private static final String SEPARATOR = "@";

    public static String getIdentifier(RequestMapping pAccess, RequestMapping pClassMapping) {
        if (pAccess != null) {
            if (pClassMapping != null) {
                return pClassMapping.value()[0] + pAccess.value()[0] + SEPARATOR + pAccess.method()[0];
            }
            else {
                return pAccess.value()[0] + SEPARATOR + pAccess.method()[0];
            }
        }
        return null;
    }

}
