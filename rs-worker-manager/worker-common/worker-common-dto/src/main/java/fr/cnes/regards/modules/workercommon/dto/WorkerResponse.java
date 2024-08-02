/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workercommon.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response sent by workers to the manager with request status.
 *
 * @author Sébastien Binda
 */
public class WorkerResponse {

    private WorkerResponseStatus status;

    private List<String> messages = new ArrayList<>();

    /**
     * Additional headers to be propagated to the next worker.
     */
    private Map<String, String> additionalHeaders;

    private byte[] content;

    public WorkerResponseStatus getStatus() {
        return status;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setStatus(WorkerResponseStatus status) {
        this.status = status;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
    }

    public void setAdditionalHeaders(Map<String, String> additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
