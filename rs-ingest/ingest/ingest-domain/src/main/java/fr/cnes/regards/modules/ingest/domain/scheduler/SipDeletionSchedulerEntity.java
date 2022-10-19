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
package fr.cnes.regards.modules.ingest.domain.scheduler;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * This entity store the last date analysed by the scheduler.
 * It will be useful to optimize deletion request avoiding delete multiple times the same sip.
 *
 * @author Thomas GUILLOU
 **/
@Entity
@Table(name = "t_sip_deletion_scheduler")
public class SipDeletionSchedulerEntity {

    @Id
    @SequenceGenerator(name = "sipDeletionSchedulerSequence", sequenceName = "seq_sip_deletion_scheduler",
        initialValue = 1)
    @GeneratedValue(generator = "sipDeletionSchedulerSequence")
    private Long id;

    @Column(name = "last_scheduled_date", nullable = false)
    private OffsetDateTime lastScheduledDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getLastScheduledDate() {
        return lastScheduledDate;
    }

    public void setLastScheduledDate(OffsetDateTime lastScheduledDate) {
        this.lastScheduledDate = lastScheduledDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SipDeletionSchedulerEntity that = (SipDeletionSchedulerEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(lastScheduledDate, that.lastScheduledDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SipDeletionSchedulerEntity{" + "id=" + id + ", lastScheduledDate=" + lastScheduledDate + '}';
    }
}
