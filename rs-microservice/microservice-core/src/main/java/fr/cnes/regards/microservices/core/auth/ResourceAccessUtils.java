package fr.cnes.regards.microservices.core.auth;

import org.springframework.web.bind.annotation.RequestMapping;

public class ResourceAccessUtils {

    private static final String SEPARATOR = "@";

    public ResourceAccessUtils() {
        super();
    }

    public static String getIdentifier(RequestMapping access, RequestMapping classMapping) {
        if (access != null) {
            if (classMapping != null) {
                return classMapping.value()[0] + access.value()[0] + SEPARATOR + access.method()[0];
            }
            else {
                return access.value()[0] + SEPARATOR + access.method()[0];
            }
        }
        return null;
    }

}
