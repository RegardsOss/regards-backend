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
package fr.cnes.regards.modules.notification.rest.dto;

/**
 * @author sbinda
 */
public class NotificationSummary {

    private long unreads = 0L;

    private long reads = 0L;

    public NotificationSummary(Long unreads, Long reads) {
        super();
        if (unreads != null) {
            this.unreads = unreads;
        }
        if (reads != null) {
            this.reads = reads;
        }
    }

    public long getUnreads() {
        return unreads;
    }

    public void setUnreads(long unreads) {
        this.unreads = unreads;
    }

    public long getReads() {
        return reads;
    }

    public void setReads(long reads) {
        this.reads = reads;
    }

}
