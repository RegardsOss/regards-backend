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


package fr.cnes.regards.modules.ingest.domain.dump;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 *
 * @author Iliana Ghazali
 */
@Entity
@Table(name = "t_dump")
public class LastDump {

    // lastDumpDate to be updated when a dump is created
    public static final long LAST_DUMP_DATE_ID = 0;

    @Id
    private long id;

    @Column(name = "last_dump_req_date", nullable = false)
    private OffsetDateTime lastDumpReqDate;

    public LastDump(OffsetDateTime lastDumpReqDate) {
        this.id = LAST_DUMP_DATE_ID;
        this.lastDumpReqDate = lastDumpReqDate;
    }

    public LastDump() {
    }

    public OffsetDateTime getLastDumpReqDate() {
        return this.lastDumpReqDate;
    }

    public void setLastDumpReqDate(OffsetDateTime lastDumpReqDate) {
        this.lastDumpReqDate = lastDumpReqDate;
    }
}
