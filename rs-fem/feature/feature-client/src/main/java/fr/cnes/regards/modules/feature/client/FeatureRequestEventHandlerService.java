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
package fr.cnes.regards.modules.feature.client;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for {@link FeatureRequestEventHandler}
 **/
@Service
public class FeatureRequestEventHandlerService {

    @MultitenantTransactional
    public void handle(List<FeatureRequestEvent> events, IFeatureRequestEventListener featureRequestEventListener) {
        List<FeatureRequestEvent> denied = new ArrayList<>();
        List<FeatureRequestEvent> granted = new ArrayList<>();
        List<FeatureRequestEvent> success = new ArrayList<>();
        List<FeatureRequestEvent> error = new ArrayList<>();
        // dispatch event according to which state caused them to be sent
        for (FeatureRequestEvent event : events) {
            switch (event.getState()) {
                case DENIED -> denied.add(event);
                case GRANTED -> granted.add(event);
                case SUCCESS -> success.add(event);
                case ERROR -> error.add(event);
            }
        }
        // now manage message in right order
        if (!denied.isEmpty()) {
            featureRequestEventListener.onRequestDenied(denied);
        }
        if (!granted.isEmpty()) {
            featureRequestEventListener.onRequestGranted(granted);
        }
        if (!error.isEmpty()) {
            featureRequestEventListener.onRequestError(error);
        }
        if (!success.isEmpty()) {
            featureRequestEventListener.onRequestSuccess(success);
        }
    }
}
