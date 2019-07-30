/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.stream.Collectors;

/**
 * Exception indicating an entity is invalid
 * @author CS
 * @author Sylvain Vissiere-Guerinet
 */
@SuppressWarnings("serial")
public class EntityInvalidException extends EntityException {

    /**
     * Detailed messages
     */
    private final List<String> messages = new ArrayList<>();

    /**
     * Constructor setting the exception message
     */
    public EntityInvalidException(final String message) {
        super(message);
        this.messages.add(message);
    }

    public EntityInvalidException(final List<String> messages) {
        super(messages.stream().collect(Collectors.joining(" ", "Invalid entity: ", "")));
        this.messages.addAll(messages);
    }

    public EntityInvalidException(String message, Throwable cause) {
        super(message, cause);
        this.messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }
}
