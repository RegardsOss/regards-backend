/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This class allows to inject public authentication header dynamically.
 *
 * @author Marc Sordi
 *
 */
public class CustomHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * Custom dynamic header
     */
    private final Map<String, String> customHeaders;

    public CustomHttpServletRequest(HttpServletRequest request) {
        super(request);
        customHeaders = new HashMap<>();
    }

    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // Look up in custom headers
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        } else {
            return super.getHeader(name);
        }
    }

}
