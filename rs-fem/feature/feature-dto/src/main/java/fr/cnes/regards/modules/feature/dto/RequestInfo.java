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
package fr.cnes.regards.modules.feature.dto;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * REST feature request response information
 *
 * @param <ID> String or {@link FeatureUniformResourceName} according to the context
 * @author Marc SORDI
 */
public class RequestInfo<ID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInfo.class);

    /**
     * Mapping between feature id or URN and request id
     */
    private Map<ID, String> granted = new HashMap<>();

    /**
     * Mapping between id and denied reason
     */
    private Multimap<ID, String> denied = ArrayListMultimap.create();

    private List<String> messages = new ArrayList<>();

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void addGrantedRequest(ID id, String requestId) {
        LOGGER.info("Granted request with id {} and request id {}", id, requestId);
        granted.put(id, requestId);
    }

    public void addDeniedRequest(ID id, String reason) {
        LOGGER.info("Denied request {} because \"{}\"", id, reason);
        denied.put(id, reason);
    }

    public void addDeniedRequest(ID id, Iterable<String> reasons) {
        String aggregatedReasons = "Unknown reason";
        if (reasons != null) {
            StringJoiner joiner = new StringJoiner(", ");
            reasons.iterator().forEachRemaining(reason -> joiner.add(reason));
            aggregatedReasons = joiner.toString();
        }
        LOGGER.error("Denied request {} because \"{}\"", id, aggregatedReasons);
        denied.putAll(id, reasons);
    }

    public Map<ID, String> getGranted() {
        return granted;
    }

    public Multimap<ID, String> getDenied() {
        return denied;
    }

    public void setGranted(Map<ID, String> granted) {
        this.granted = granted;
    }

    public void setDenied(Multimap<ID, String> denied) {
        this.denied = denied;
    }
}
