/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingCursor.class);

    @Column(name = "cursor_position")
    @Min(0)
    private int position;

    /**
     * The previous iteration value of {@link CrawlingCursor#lastEntityDate}.
     * Used to deduct if the crawler do not found any new product after 2 aspirations
     */
    @Column(name = "cursor_previous_last_entity_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime previousLastEntityDate;

    /**
     * The date of the last object retrieved in the previous iteration.
     * Can be null if last entities date are not provided.
     */
    @Column(name = "cursor_last_entity_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastEntityDate;

    @Column(name = "cursor_last_id")
    private Long lastId;

    @Column(name = "previous_cursor_last_id")
    private Long previousLastId;

    @Transient
    private int size;

    @Transient
    private boolean hasNext;

    @Transient
    private Long currentLastId;

    @Transient
    private OffsetDateTime currentLastEntityDate;

    public CrawlingCursor() {
        // for hibernate
        this(0, 1, null, null, null);
    }

    public CrawlingCursor(int position, int size) {
        this(position, size, null, null, null);
    }

    public CrawlingCursor(OffsetDateTime lastEntityDate) {
        this(0, 1, null, lastEntityDate, null);
    }

    private CrawlingCursor(int position,
                           int size,
                           OffsetDateTime currentLastEntityDate,
                           OffsetDateTime lastEntityDate,
                           Long lastId) {
        Assert.isTrue(size > 0, "Cursor size should be greater than 0");
        Assert.isTrue(position >= 0, "Position should be greater than 0");
        this.hasNext = false;
        this.size = size;
        this.position = position;
        this.currentLastEntityDate = currentLastEntityDate;
        this.lastEntityDate = lastEntityDate;
        this.lastId = lastId;
    }

    public CrawlingCursor(Long lastId) {
        this(0, 1, null, null, lastId);
    }

    public void next() {
        next(CrawlingCursorMode.CRAWL_SINCE_LAST_UPDATE);
    }

    /**
     * Iterate the current cursor to get the next page to retrieve.
     * By default, the next page to retrieve is previousPageNumber + 1.
     * If the {@link #currentLastEntityDate} is not provided:
     * <ul>
     *   <li>increment page number</li>
     * </ul>
     * Otherwise:
     * <ul>
     *   <li>reset the page number only if it is different from the previous one -> the next page will be the first page with date >= currentPageMaxUpdate</li>
     *   <li>else increment the page number if the current max update date is equal to the previous one</li>
     * </ul>
     */
    public void next(CrawlingCursorMode mode) {
        if (hasNext) {
            switch (mode) {
                case CRAWL_EVERYTHING -> position++;
                case CRAWL_SINCE_LAST_UPDATE -> nextFromDate();
                case CRAWL_FROM_LAST_ID -> nextFromIncrement();
                default -> throw new IllegalStateException("Unexpected crawling mode: " + mode);
            }
        } else {
            throw new IllegalStateException("This cursor does not have a next position!");
        }
    }

    private void nextFromIncrement() {
        if (currentLastId == null) {
            position++;
        } else {
            if (!currentLastId.equals(lastId)) {
                position = 0;
            } else {
                position++;
            }
            lastId = currentLastId;
            currentLastId = null;
        }
        LOGGER.debug("Next crawling cursor : {}", this);
    }

    private void nextFromDate() {
        if (currentLastEntityDate == null) {
            position++;
        } else {
            if (lastEntityDate == null || !currentLastEntityDate.isEqual(lastEntityDate)) {
                position = 0;
            } else {
                position++;
            }
            lastEntityDate = currentLastEntityDate;
            currentLastEntityDate = null;
        }
        LOGGER.debug("Next crawling cursor : {}", this);
    }

    /**
     * To prevent data loss, an overlap time set in datasource configuration is deducted
     * from the previous date in order to harvest again a critical period of time.
     *
     * @param overlapInSecond overlap in second
     */
    public void tryApplyOverlap(long overlapInSecond) {
        // If and only if a harvesting process has already taken place
        if (lastEntityDate != null) {
            LOGGER.debug("Starting analysing previous date in current crawling cursor : {}", this);
            if (previousLastEntityDate != null && previousLastEntityDate.equals(lastEntityDate)) {
                // Harvesting loops on same second
                // Advance by one second in order not to eternally harvest the last element(s) of this second
                lastEntityDate = lastEntityDate.plusSeconds(1);
                LOGGER.debug("Previous date advanced by one second in current crawling cursor : {}", this);
            } else {
                // Backup last entity date in previous one to keep 2 level date history
                previousLastEntityDate = lastEntityDate;
                // Apply overlap
                lastEntityDate = lastEntityDate.minusSeconds(overlapInSecond);
                LOGGER.debug("Overlap of {}s applied to previous date in current crawling cursor : {}",
                             overlapInSecond,
                             this);
            }

        } else if (lastId != null) {
            if (previousLastId != null && previousLastId.equals(lastId)) {
                // Harvesting loops on same id
                // Advance by one in order not to eternally harvest the last element(s)
                lastId += 1;
                LOGGER.debug("Previous id advanced by one in current crawling cursor : {}", this);
            } else {
                previousLastId = lastId;
            }
        } else {
            LOGGER.debug("Overlap not applicable to current crawling cursor : {}", this);
        }
    }

    public OffsetDateTime getCurrentLastEntityDate() {
        return currentLastEntityDate;
    }

    public void setCurrentLastEntityDate(OffsetDateTime currentLastEntityDate) {
        if (currentLastEntityDate != null) {
            this.currentLastEntityDate = currentLastEntityDate.truncatedTo(ChronoUnit.MICROS);
        }
    }

    public OffsetDateTime getLastEntityDate() {
        return lastEntityDate;
    }

    public OffsetDateTime getPreviousLastEntityDate() {
        return previousLastEntityDate;
    }

    public Long getLastId() {
        return lastId;
    }

    public void setLastId(Long lastId) {
        this.lastId = lastId;
    }

    public Long getCurrentLastId() {
        return currentLastId;
    }

    public void setCurrentLastId(Long currentLastId) {
        this.currentLastId = currentLastId;
    }

    public void setPreviousLastEntityDate(OffsetDateTime previousLastEntityDate) {
        this.previousLastEntityDate = previousLastEntityDate;
    }

    /**
     * @param lastEntityDate not null last entity date
     */
    public void setLastEntityDate(OffsetDateTime lastEntityDate) {
        this.lastEntityDate = lastEntityDate.truncatedTo(ChronoUnit.MICROS);
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

    public Long getPreviousLastId() {
        return previousLastId;
    }

    public void setPreviousLastId(Long previousLastId) {
        this.previousLastId = previousLastId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CrawlingCursor that = (CrawlingCursor) o;
        return position == that.position
               && size == that.size
               && hasNext == that.hasNext
               && Objects.equals(previousLastEntityDate,
                                 that.previousLastEntityDate)
               && Objects.equals(lastEntityDate, that.lastEntityDate)
               && Objects.equals(lastId, that.lastId)
               && Objects.equals(previousLastId, that.previousLastId)
               && Objects.equals(currentLastId, that.currentLastId)
               && Objects.equals(currentLastEntityDate, that.currentLastEntityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position,
                            previousLastEntityDate,
                            lastEntityDate,
                            lastId,
                            previousLastId,
                            size,
                            hasNext,
                            currentLastId,
                            currentLastEntityDate);
    }

    @Override
    public String toString() {
        return "CrawlingCursor{"
               + "position="
               + position
               + ", previousLastEntityDate="
               + previousLastEntityDate
               + ", lastEntityDate="
               + lastEntityDate
               + ", lastId="
               + lastId
               + ", previousLastId="
               + previousLastId
               + '\''
               + ", size="
               + size
               + ", hasNext="
               + hasNext
               + ", currentLastId="
               + currentLastId
               + ", currentLastEntityDate="
               + currentLastEntityDate
               + '}';
    }
}
