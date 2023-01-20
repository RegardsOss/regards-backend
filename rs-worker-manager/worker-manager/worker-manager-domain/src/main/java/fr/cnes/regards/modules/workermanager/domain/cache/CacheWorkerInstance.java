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
package fr.cnes.regards.modules.workermanager.domain.cache;

import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerHeartBeatEvent;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * An instance of worker in the cache
 *
 * @author Léo Mieulet
 */
public class CacheWorkerInstance {

    /**
     * Worker ID
     */
    private final String id;

    /**
     * Date used by the last heart beat
     */
    private final OffsetDateTime lastHeartBeatDate;

    private final String workerType;

    public CacheWorkerInstance(String id, String workerType, @NotNull OffsetDateTime heartBeatDate) {
        this.id = id;
        this.lastHeartBeatDate = heartBeatDate;
        this.workerType = workerType;
    }

    public static CacheWorkerInstance build(WorkerHeartBeatEvent workerHeartBeatEvent) {
        return new CacheWorkerInstance(workerHeartBeatEvent.getId(),
                                       workerHeartBeatEvent.getType(),
                                       workerHeartBeatEvent.getHeartBeatDate());
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getLastHeartBeatDate() {
        return lastHeartBeatDate;
    }

    public String getWorkerType() {
        return workerType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheWorkerInstance that = (CacheWorkerInstance) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
