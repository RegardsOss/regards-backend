/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.feature.domain.settings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Model to handle optional notifications
 * @author Iliana Ghazali
 */

@Entity
@Table(name = "t_feature_notification_settings")
public class FeatureNotificationSettings {

    // only one setting per tenant
    private static final long FEM_NOTIFICATION_SETTINGS_ID = 0L;

    @Id
    @Column(name = "id", unique = true)
    private final Long id;

    /**
     * To activate notifications on feature requests
     */
    @Column(name = "active_notifications", nullable = false)
    private boolean activeNotification = true;

    public boolean isActiveNotification() {
        return activeNotification;
    }

    public void setActiveNotification(boolean activeNotification) {
        this.activeNotification = activeNotification;
    }

    public FeatureNotificationSettings() {
        this.id = FEM_NOTIFICATION_SETTINGS_ID;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureNotificationSettings that = (FeatureNotificationSettings) o;
        return activeNotification == id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activeNotification);
    }
}
