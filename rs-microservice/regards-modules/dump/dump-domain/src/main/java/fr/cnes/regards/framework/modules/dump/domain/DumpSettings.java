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

package fr.cnes.regards.framework.modules.dump.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Model to handle dump settings
 * @author Iliana Ghazali
 */
@Entity
@Table(name = "t_dump_settings")
public class DumpSettings {

    // only one configuration per tenant
    public static final long DUMP_CONF_ID = 0;

    @Id
    @Column(name ="id")
    private Long id;

    @Column(name = "active_module", nullable = false)
    private boolean activeModule;

    @Column(name = "cron_trigger", nullable = false)
    private String cronTrigger;

    @Column(name = "dump_location")
    private String dumpLocation;

    @Column(name = "last_dump_req_date")
    private OffsetDateTime lastDumpReqDate;

    public DumpSettings(boolean activeModule, String cronTrigger, String dumpLocation,
            OffsetDateTime lastDumpReqDate) {
        this.id = DUMP_CONF_ID;
        this.activeModule = activeModule;
        this.cronTrigger = cronTrigger;
        this.dumpLocation = dumpLocation;
        this.lastDumpReqDate = lastDumpReqDate;
    }

    public DumpSettings() {
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getLastDumpReqDate() {
        return this.lastDumpReqDate;
    }

    public void setLastDumpReqDate(OffsetDateTime lastDumpReqDate) {
        this.lastDumpReqDate = lastDumpReqDate;
    }

    public boolean isActiveModule() {
        return activeModule;
    }

    public void setActiveModule(boolean activeModule) {
        this.activeModule = activeModule;
    }

    public String getCronTrigger() {
        return cronTrigger;
    }

    public void setCronTrigger(String cronTrigger) {
        this.cronTrigger = cronTrigger;
    }

    public String getDumpLocation() {
        return dumpLocation;
    }

    public void setDumpLocation(String dumpLocation) {
        this.dumpLocation = dumpLocation;
    }
}