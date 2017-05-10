/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.representation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Generic response body with containing a single message and key value pairs
 *
 * @author Marc Sordi
 *
 */
public class GenericResponseBody {

    private String message;

    private final Map<String, Object> properties = new HashMap<>();

    public GenericResponseBody() {
        // Default constructor
    }

    public GenericResponseBody(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setMessage(String pMessage) {
        message = pMessage;
    }
}
