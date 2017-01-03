/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Class InvalidEntityException
 *
 * Exception to indicates that the entity requested is invalid.
 *
 * @author CS
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
 */
public class EntityInvalidException extends EntityException {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1677039769133438679L;

    /**
     * Detailed messages
     */
    private final List<String> messages;

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            Entity error message
     * @since 1.0-SNAPSHOT
     */
    public EntityInvalidException(final String pMessage) {
        super(pMessage);
        this.messages = new ArrayList<>();
        this.messages.add(pMessage);
    }

    public EntityInvalidException(final List<String> pMessages) {
        super("Invalid entity");
        this.messages = pMessages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
