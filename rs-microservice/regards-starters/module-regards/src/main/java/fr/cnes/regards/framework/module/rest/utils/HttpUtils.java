/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.utils;

import org.springframework.http.HttpStatus;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public final class HttpUtils {

    private static final int HTTP_CODE_CLASS_MULTIPLIER = 100;

    private HttpUtils() {
        // private constructor of a util class
    }

    public static boolean isSuccess(HttpStatus pHttpStatus) {
        return (pHttpStatus.value() / HTTP_CODE_CLASS_MULTIPLIER) == 2;
    }

}
