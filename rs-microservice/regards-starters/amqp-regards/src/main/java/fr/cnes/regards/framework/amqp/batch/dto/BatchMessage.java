/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.batch.dto;

import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import org.springframework.amqp.core.Message;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Keep track of AMQP origin message
 *
 * @author Marc SORDI
 */
public final class BatchMessage {

    private final Message origin;

    @Nullable
    private final Object converted;

    private BatchMessage(Message origin, @Nullable Object converted) {
        this.origin = origin;
        this.converted = converted;
    }

    public static BatchMessage buildConvertedBatchMessage(Message origin, Object converted) {

        // Propagate message properties if required
        if (IMessagePropertiesAware.class.isAssignableFrom(converted.getClass())) {
            ((IMessagePropertiesAware) converted).setMessageProperties(origin.getMessageProperties());
        }

        return new BatchMessage(origin, converted);
    }

    public static BatchMessage buildNotConvertedBatchMessage(Message origin) {
        return new BatchMessage(origin, null);
    }

    public Message getOrigin() {
        return origin;
    }

    @Nullable
    public Object getConverted() {
        return converted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BatchMessage that = (BatchMessage) o;
        return Objects.equals(origin, that.origin) && Objects.equals(converted, that.converted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, converted);
    }

    @Override
    public String toString() {
        return "BatchMessage{" + "origin=" + origin + ", converted=" + converted + '}';
    }
}