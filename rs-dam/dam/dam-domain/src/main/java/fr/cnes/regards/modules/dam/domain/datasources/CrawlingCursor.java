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
package fr.cnes.regards.modules.dam.domain.datasources;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Cursor to retrieve a bunch of elements.
 *
 * @author Iliana Ghazali
 * @author Sylvain Vissiere-Guerinet
 **/
@Embeddable
public class CrawlingCursor {

    @Column(name = "cursor_position")
    @Min(0)
    private int position;

    /**
     * The date of the last object retrieved in the previous iteration.
     * Can be null if last entities date are not provided.
     */
    @Column(name = "cursor_previous_last_entity_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime previousLastEntityDate;

    @Transient
    private int size;

    @Transient
    private boolean hasNext;

    @Transient
    private OffsetDateTime lastEntityDate;

    public CrawlingCursor() {
        // for hibernate
        this(0, 1, null, null);
    }

    public CrawlingCursor(int position, int size) {
        this(position, size, null, null);
    }

    public CrawlingCursor(OffsetDateTime previousLastEntityDate) {
        this(0, 1, null, previousLastEntityDate);
    }

    private CrawlingCursor(int position,
                          int size,
                          OffsetDateTime lastEntityDate,
                          OffsetDateTime previousLastEntityDate) {
        Assert.isTrue(size > 0, "Cursor size should be greater than 0");
        Assert.isTrue(position >= 0, "Position should be greater than 0");
        this.hasNext = false;
        this.size = size;
        this.position = position;
        this.lastEntityDate = lastEntityDate;
        this.previousLastEntityDate = previousLastEntityDate;
    }

    /**
     * Iterate the current cursor to get the next page to retrieve.
     * By default, the next page to retrieve is previousPageNumber + 1.
     * If the {@link #lastEntityDate} is not provided:
     * <ul>
     *   <li>increment page number</li>
     * </ul>
     * Otherwise:
     * <ul>
     *   <li>reset the page number only if it is different from the previous one -> the next page will be the first page with date >= currentPageMaxUpdate</li>
     *   <li>else increment the page number if the current max update date is equal to the previous one</li>
     * </ul>
     */
    public void next() {
        if (hasNext) {
            if (lastEntityDate == null) {
                position++;
            } else {
                if (previousLastEntityDate == null || !lastEntityDate.isEqual(previousLastEntityDate)) {
                    position = 0;
                } else {
                    position++;
                }
                previousLastEntityDate = lastEntityDate;
                lastEntityDate = null;
            }
        } else {
            throw new IllegalStateException("This cursor does not have a next position!");
        }
    }

    public OffsetDateTime getLastEntityDate() {
        return lastEntityDate;
    }

    public void setLastEntityDate(OffsetDateTime lastEntityDate) {
        this.lastEntityDate = lastEntityDate.truncatedTo(ChronoUnit.MICROS);
    }

    public OffsetDateTime getPreviousLastEntityDate() {
        return previousLastEntityDate;
    }

    public void setPreviousLastEntityDate(OffsetDateTime previousLastEntityDate) {
        this.previousLastEntityDate = previousLastEntityDate.truncatedTo(ChronoUnit.MICROS);
    }

    public int getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrawlingCursor that)) {
            return false;
        }

        return position == that.position
               && size == that.size
               && previousLastEntityDate.isEqual(that.previousLastEntityDate)
               && lastEntityDate.isEqual(that.lastEntityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, size, previousLastEntityDate, lastEntityDate);
    }

    @Override
    public String toString() {
        return "CrawlingCursor{"
               + "position="
               + position
               + ", size="
               + size
               + ", hasNext="
               + hasNext
               + ", previousLastEntityDate="
               + previousLastEntityDate
               + ", lastEntityDate="
               + lastEntityDate
               + '}';
    }
}
