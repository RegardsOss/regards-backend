/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dto.requests;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.amqp.core.Message;

import java.util.*;
import java.util.stream.Collectors;

public class SessionsRequestsInfo {

    private final Map<Pair<String, String>, RequestsInfo> infosPerSession = new HashMap<>();

    private final List<Message> skippedEvents = Lists.newArrayList();

    public SessionsRequestsInfo() {
        super();
    }

    public SessionsRequestsInfo(Collection<RequestDTO> requests) {
        super();
        this.addRequests(requests);
    }

    private static Pair<String, String> getKey(RequestDTO request) {
        return Pair.of(request.getSource(), request.getSession());
    }

    public Set<Pair<String, String>> keySet() {
        return infosPerSession.keySet();
    }

    public RequestsInfo get(Pair<String, String> key) {
        return this.infosPerSession.getOrDefault(key, new RequestsInfo());
    }

    public final void addRequests(Collection<RequestDTO> requests) {
        requests.forEach(this::addRequest);
    }

    public Set<RequestDTO> addRequest(RequestDTO request) {
        Map<RequestStatus, Set<RequestDTO>> requestsByStatuses = infosPerSession.compute(getKey(request),
                                                                                         (sessionKey, ri) -> ri
                                                                                                             == null ?
                                                                                             new RequestsInfo() :
                                                                                             ri).getRequests();
        Set<RequestDTO> statusRequests = requestsByStatuses.compute(request.getStatus(),
                                                                    (status, requests) -> requests == null ?
                                                                        Sets.newHashSet() :
                                                                        requests);
        statusRequests.add(request);
        return statusRequests;
    }

    public Collection<Message> getSkippedEvents() {
        return this.skippedEvents;
    }

    public Collection<RequestDTO> getRequests(RequestStatus status) {
        return infosPerSession.values()
                              .stream()
                              .flatMap(r -> r.getRequests()
                                             .compute(status,
                                                      (key, requests) -> requests == null ?
                                                          Sets.newHashSet() :
                                                          requests)
                                             .stream())
                              .collect(Collectors.toList());
    }

}
