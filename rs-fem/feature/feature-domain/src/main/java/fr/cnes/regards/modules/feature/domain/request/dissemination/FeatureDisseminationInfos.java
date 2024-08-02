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
package fr.cnes.regards.modules.feature.domain.request.dissemination;

import com.google.common.collect.Sets;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class FeatureDisseminationInfos {

    private final Map<FeatureUpdateDisseminationInfoType, Set<FeatureUpdateDisseminationRequest>> requestsByDisseminationType = new EnumMap<>(
        FeatureUpdateDisseminationInfoType.class);

    public Set<FeatureUpdateDisseminationRequest> get(FeatureUpdateDisseminationInfoType type) {
        return requestsByDisseminationType.getOrDefault(type, new HashSet<>());
    }

    public void addRequest(FeatureUpdateDisseminationRequest request) {
        Set<FeatureUpdateDisseminationRequest> statusRequests = requestsByDisseminationType.compute(request.getUpdateType(),
                                                                                                    (status, requests) ->
                                                                                                        requests
                                                                                                        == null ?
                                                                                                            Sets.newHashSet() :
                                                                                                            requests);

        statusRequests.add(request);
    }
}
