/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.database;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author sbinda
 *
 */
@Entity
@Table(name = "t_storage_monitoring")
public class StorageMonitoring {

    /**
     * Only one possible entry in this table so id forced to 0
     */
    @Id
    private final Long id = 0L;

    @Column(nullable = false)
    private boolean running;

    @Column(name = "last_monitoring_date")
    private OffsetDateTime lastMonitoringDate;

    @Column(name = "last_file_reference_id")
    private Long lastFileReferenceIdMonitored = 0L;

    @Column(name = "last_monitoring_duration")
    private Long lastMonitoringDuration;

    public StorageMonitoring() {
        super();
    }

    public StorageMonitoring(boolean running, OffsetDateTime lastMonitoringDate, Long lastFileReferenceIdMonitored,
            Long lastMonitoringDuration) {
        super();
        this.running = running;
        this.lastMonitoringDate = lastMonitoringDate;
        this.lastFileReferenceIdMonitored = lastFileReferenceIdMonitored;
        this.lastMonitoringDuration = lastMonitoringDuration;
    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return the lastMonitoringDate
     */
    public OffsetDateTime getLastMonitoringDate() {
        return lastMonitoringDate;
    }

    /**
     * @param lastMonitoringDate the lastMonitoringDate to set
     */
    public void setLastMonitoringDate(OffsetDateTime lastMonitoringDate) {
        this.lastMonitoringDate = lastMonitoringDate;
    }

    /**
     * @return the lastFileReferenceIdMonitored
     */
    public Long getLastFileReferenceIdMonitored() {
        return lastFileReferenceIdMonitored;
    }

    /**
     * @param lastFileReferenceIdMonitored the lastFileReferenceIdMonitored to set
     */
    public void setLastFileReferenceIdMonitored(Long lastFileReferenceIdMonitored) {
        this.lastFileReferenceIdMonitored = lastFileReferenceIdMonitored;
    }

    /**
     * @return the lastMonitoringDuration
     */
    public Long getLastMonitoringDuration() {
        return lastMonitoringDuration;
    }

    /**
     * @param lastMonitoringDuration the lastMonitoringDuration to set
     */
    public void setLastMonitoringDuration(Long lastMonitoringDuration) {
        this.lastMonitoringDuration = lastMonitoringDuration;
    }

}
