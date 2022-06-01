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
package fr.cnes.regards.modules.feature.domain.request.dissemination;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class SessionFeatureDisseminationInfos {

    // The Map containing as key the sessionOwner, the session and the recipient label
    private final Map<Triple<String, String, String>, FeatureDisseminationInfos> infosPerSessionAndRecipient = new HashMap<>();

    private static Triple<String, String, String> getKey(FeatureEntity featureEntity,
                                                         FeatureUpdateDisseminationRequest request) {
        return Triple.of(featureEntity.getSessionOwner(), featureEntity.getSession(), request.getRecipientLabel());
    }

    public Set<Triple<String, String, String>> keySet() {
        return infosPerSessionAndRecipient.keySet();
    }

    public FeatureDisseminationInfos get(Triple<String, String, String> key) {
        return this.infosPerSessionAndRecipient.getOrDefault(key, new FeatureDisseminationInfos());
    }

    public void addRequest(FeatureEntity featureEntity, FeatureUpdateDisseminationRequest request) {

        FeatureDisseminationInfos featureDisseminationInfos = infosPerSessionAndRecipient.compute(getKey(featureEntity,
                                                                                                         request),
                                                                                                  (sessionKey, ri) -> ri
                                                                                                                      == null ?
                                                                                                      new FeatureDisseminationInfos() :
                                                                                                      ri);

        featureDisseminationInfos.addRequest(request);
    }
}
