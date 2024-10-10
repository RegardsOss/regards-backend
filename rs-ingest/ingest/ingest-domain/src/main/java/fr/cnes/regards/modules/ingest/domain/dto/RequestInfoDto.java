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
package fr.cnes.regards.modules.ingest.domain.dto;

import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;

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
    @Schema(description = "Map of key/value where key is the SIP generated URN and value is the request correlation "
                          + "id.", example = "{ \"URN:SIP:DATA:xxxxxxx:V1\": \"119\", "
                                             + "\"URN:SIP:DATA:yyyyyyyy:V1\": \"120\" }")
    private final ConcurrentMap<String, String> granted = new ConcurrentHashMap<>();

    /**
     * Mapping between SIP id and denied reason
     */
    @Schema(description = "Map of key/value where key is the SIP providerId and value is deny reason message.",
            example = "{\"providerId_001\" : \"SIP malformed\"}")
    private final ConcurrentMap<String, String> denied = new ConcurrentHashMap<>();

    @Schema(description = "Global status message.")
    private List<String> messages;

    @Schema(description = "Owner of the submission requests session", example = "Jean")
    private String sessionOwner;

    @Schema(description = "Session of the submission requests", example = "Products of January 2024")
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
