/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.module.manager;

import java.util.ArrayList;
import java.util.List;

/**
 * A report for module management
 *
 * @author Marc SORDI
 *
 */
public class ModuleRestartReport {

    private final ModuleInformation moduleInformation;

    private List<String> messages;

    /**
     * @return the messages
     */
    public List<String> getMessages() {
        return messages;
    }

    public ModuleRestartReport(ModuleInformation moduleInformation) {
        this.moduleInformation = moduleInformation;
    }

    public void addMessage(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    /**
     * @return the moduleInformation
     */
    public ModuleInformation getModuleInformation() {
        return moduleInformation;
    }
}
