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
package fr.cnes.regards.modules.feature.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureNotificationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;

/**
 * Service for notify {@link Feature}
 * @author Kevin Marchois
 *
 */
public interface IFeatureNotificationService extends IAbstractFeatureService<FeatureNotificationRequest> {

    /**
     * Register notification requests in database for further processing from incoming request events
     */
    int registerRequests(List<FeatureNotificationRequestEvent> events);

    int sendToNotifier();

    void handleNotificationSuccess(Set<AbstractFeatureRequest> success);

    void handleNotificationError(Set<AbstractFeatureRequest> errorRequest, FeatureRequestStep errorStep);

    /**
     * Find all {@link FeatureNotificationRequest}s
     * @return {@link FeatureNotificationRequest}s
     */
    Page<FeatureNotificationRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);
}
