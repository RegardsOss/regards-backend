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
package fr.cnes.regards.modules.notifier.dto.out;

import java.util.Objects;

/**
 * Information of recipient
 */
public class Recipient {

    private final String label;

    private final RecipientStatus status;

    private final boolean ackRequired;

    private final boolean blockingRequired;

    public Recipient(String label, RecipientStatus status, boolean ackRequired, boolean blockingRequired) {
        this.label = label;
        this.status = status;
        this.ackRequired = ackRequired;
        this.blockingRequired = blockingRequired;

        if (blockingRequired && !ackRequired) {
            throw new IllegalArgumentException(String.format("If a recipient is required the "
                                                             + "blocking[blockingRequired: %s], "
                                                             + "then the acknowledgment[ackRequired: %s] of "
                                                             + "recipient is required.",
                                                             blockingRequired,
                                                             ackRequired));
        }
    }

    public String getLabel() {
        return label;
    }

    public RecipientStatus getStatus() {
        return status;
    }

    public boolean isAckRequired() {
        return ackRequired;
    }

    public boolean isBlockingRequired() {
        return blockingRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recipient)) {
            return false;
        }
        Recipient recipient = (Recipient) o;
        return Objects.equals(label, recipient.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

}
