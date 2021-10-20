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
package fr.cnes.regards.modules.notifier.dto.out;

import java.util.Objects;

public class Recipient {

    private String id;
    private RecipientStatus status;
    private boolean ackRequired;

    public Recipient() {
    }

    public Recipient(String id, RecipientStatus status, boolean ackRequired) {
        this.id = id;
        this.status = status;
        this.ackRequired = ackRequired;
    }

    public String getId() {
        return id;
    }

    public Recipient setId(String id) {
        this.id = id;
        return this;
    }

    public RecipientStatus getStatus() {
        return status;
    }

    public Recipient setStatus(RecipientStatus status) {
        this.status = status;
        return this;
    }

    public boolean isAckRequired() {
        return ackRequired;
    }

    public Recipient setAckRequired(boolean ackRequired) {
        this.ackRequired = ackRequired;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipient)) return false;
        Recipient recipient = (Recipient) o;
        return Objects.equals(id, recipient.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
