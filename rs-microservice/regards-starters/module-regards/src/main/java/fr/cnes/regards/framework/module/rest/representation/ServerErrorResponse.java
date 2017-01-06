/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server error response representation
 *
 * @author Marc Sordi
 *
 *         TODO : generalize this representation
 */
public class ServerErrorResponse {

    /**
     * Error message
     */
    private final List<String> messages;

    public ServerErrorResponse(String pMessage) {
        this.messages = new ArrayList<>();
        this.messages.add(pMessage);
    }

    public ServerErrorResponse(List<String> pMessages) {
        this.messages = pMessages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
