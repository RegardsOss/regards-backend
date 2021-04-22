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

package fr.cnes.regards.modules.ingest.domain.settings;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;

/**
 * Model to handle optional notifications
 * @author Iliana Ghazali
 */
@Entity
@Table(name = "t_aip_notification_settings")
public class AIPNotificationSettings {

    // only one setting per tenant
    private static final long INGEST_NOTIFICATION_SETTING_ID = 0L;

    @ConfigIgnore
    @Id
    @Column(name = "id", unique = true)
    private Long id;

    /**
     * To activate notifications on aip requests
     */
    @Column(name = "active_notifications", nullable = false)
    private boolean activeNotification = false;

    public boolean isActiveNotification() {
        return activeNotification;
    }

    public void setActiveNotification(boolean activeNotification) {
        this.activeNotification = activeNotification;
    }

    public AIPNotificationSettings() {
        this.id = INGEST_NOTIFICATION_SETTING_ID;
    }

    public Long getId() {
        return id;
    }

    public void setId() {
        this.id = INGEST_NOTIFICATION_SETTING_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AIPNotificationSettings that = (AIPNotificationSettings) o;
        return (activeNotification == that.activeNotification) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activeNotification);
    }
}
