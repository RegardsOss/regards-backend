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
package fr.cnes.regards.modules.notification.domain.dto;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.notification.domain.NotificationLight;

/**
 * @author Théo Lasserre
 */
public class NotificationSpecificationBuilder
    extends AbstractSpecificationsBuilder<NotificationLight, SearchNotificationParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(useValuesRestriction("level", parameters.getLevels()));
            specifications.add(useValuesRestriction("sender", parameters.getSenders()));
            specifications.add(useValuesRestriction("status", parameters.getStatus()));
            specifications.add(before("date", parameters.getDates().getBefore()));
            specifications.add(after("date", parameters.getDates().getAfter()));
            specifications.add(useValuesRestriction("id", parameters.getIds()));
        }
    }
}
