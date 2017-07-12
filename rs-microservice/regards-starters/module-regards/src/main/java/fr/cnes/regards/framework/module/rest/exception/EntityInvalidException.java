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
