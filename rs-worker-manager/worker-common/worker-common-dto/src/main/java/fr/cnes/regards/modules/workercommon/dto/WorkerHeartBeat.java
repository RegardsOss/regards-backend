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
package fr.cnes.regards.modules.workercommon.dto;

import java.time.OffsetDateTime;

/**
 * The worker heart beat payload
 *
 * @author Léo Mieulet
 */
public abstract class WorkerHeartBeat {

    /**
     * Worker id
     */
    private String id;

    /**
     * Worker type
     */
    private String type;

    /**
     * Date when the worker sent the heartbeat
     */
    private OffsetDateTime heartBeatDate;

    public String getId() {
        return id;
    }

    public final void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public final void setType(String type) {
        this.type = type;
    }

    public OffsetDateTime getHeartBeatDate() {
        return heartBeatDate;
    }

    public final void setHeartBeatDate(OffsetDateTime heartBeatDate) {
        this.heartBeatDate = heartBeatDate;
    }
}
