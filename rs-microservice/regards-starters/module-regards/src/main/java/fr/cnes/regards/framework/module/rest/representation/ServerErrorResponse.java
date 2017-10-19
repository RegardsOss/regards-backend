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
package fr.cnes.regards.framework.module.rest.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server error response representation
 *
 * @author Marc Sordi
 *
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
