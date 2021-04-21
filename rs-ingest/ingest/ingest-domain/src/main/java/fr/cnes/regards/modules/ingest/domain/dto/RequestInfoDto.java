/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * REST ingest request response information
 *
 * @author Marc SORDI
 */
public class RequestInfoDto {

    /**
     * Mapping between SIP id and request id
     */
    private final ConcurrentMap<String, String> granted = new ConcurrentHashMap<>();

    /**
     * Mapping between SIP id and denied reason
     */
    private final ConcurrentMap<String, String> denied = new ConcurrentHashMap<>();

    private List<String> messages;

    private String sessionOwner;

    private String session;

    public static RequestInfoDto build(String sessionOwner, String session, String... messages) {
        RequestInfoDto ri = new RequestInfoDto();
        ri.setMessages(Arrays.asList(messages));
        ri.sessionOwner = sessionOwner;
        ri.session = session;
        return ri;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void addGrantedRequest(String id, String requestId) {
        granted.put(id, requestId);
    }

    public void addDeniedRequest(String id, String reason) {
        denied.put(id, reason);
    }

    public Map<String, String> getGranted() {
        return granted;
    }

    public Map<String, String> getDenied() {
        return denied;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }

}
