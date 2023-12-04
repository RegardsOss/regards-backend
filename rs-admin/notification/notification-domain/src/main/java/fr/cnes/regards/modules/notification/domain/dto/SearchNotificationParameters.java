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
package fr.cnes.regards.modules.notification.domain.dto;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

/**
 * Criterias to filter on notifications
 *
 * @author Théo Lasserre
 */
public class SearchNotificationParameters implements AbstractSearchParameters<NotificationLight> {

    @Schema(description = "Filter on notification level")
    private ValuesRestriction<NotificationLevel> levels;

    @Schema(description = "Filter on notification sender")
    private ValuesRestriction<String> senders;

    @Schema(description = "Filter on notification status")
    private ValuesRestriction<NotificationStatus> status;

    @Schema(description = "Filter on notification date")
    private DatesRangeRestriction dates = new DatesRangeRestriction();

    @Schema(description = "Filter on notification id")
    private ValuesRestriction<Long> ids;

    public ValuesRestriction<NotificationLevel> getLevels() {
        return levels;
    }

    public void setLevels(ValuesRestriction<NotificationLevel> levels) {
        this.levels = levels;
    }

    public SearchNotificationParameters withLevelsIncluded(Collection<NotificationLevel> levels) {
        this.levels = new ValuesRestriction<NotificationLevel>().withInclude(levels);
        return this;
    }

    public SearchNotificationParameters withLevelsExcluded(Collection<NotificationLevel> levels) {
        this.levels = new ValuesRestriction<NotificationLevel>().withExclude(levels);
        return this;
    }

    public ValuesRestriction<String> getSenders() {
        return senders;
    }

    public void setSenders(ValuesRestriction<String> senders) {
        this.senders = senders;
    }

    public SearchNotificationParameters withSendersIncluded(Collection<String> senders) {
        this.senders = new ValuesRestriction<String>().withInclude(senders);
        return this;
    }

    public SearchNotificationParameters withSendersExcluded(Collection<String> senders) {
        this.senders = new ValuesRestriction<String>().withExclude(senders);
        return this;
    }

    public ValuesRestriction<NotificationStatus> getStatus() {
        return status;
    }

    public void setStatus(ValuesRestriction<NotificationStatus> status) {
        this.status = status;
    }

    public SearchNotificationParameters withStatusIncluded(Collection<NotificationStatus> status) {
        this.status = new ValuesRestriction<NotificationStatus>().withInclude(status);
        return this;
    }

    public SearchNotificationParameters withStatusIncluded(NotificationStatus... status) {
        this.status = new ValuesRestriction<NotificationStatus>().withInclude(Arrays.asList(status));
        return this;
    }

    public SearchNotificationParameters withStatusExcluded(Collection<NotificationStatus> status) {
        this.status = new ValuesRestriction<NotificationStatus>().withExclude(status);
        return this;
    }

    public SearchNotificationParameters withStatusExcluded(NotificationStatus... status) {
        this.status = new ValuesRestriction<NotificationStatus>().withExclude(Arrays.asList(status));
        return this;
    }

    public DatesRangeRestriction getDates() {
        return dates;
    }

    public void setDates(DatesRangeRestriction dates) {
        this.dates = dates;
    }

    public SearchNotificationParameters withDateAfter(OffsetDateTime after) {
        this.dates.setAfter(after);
        return this;
    }

    public SearchNotificationParameters withDateBefore(OffsetDateTime before) {
        this.dates.setBefore(before);
        return this;
    }

    public ValuesRestriction<Long> getIds() {
        return ids;
    }

    public void setIds(ValuesRestriction<Long> ids) {
        this.ids = ids;
    }

    public SearchNotificationParameters withIdsIncluded(Collection<Long> ids) {
        this.ids = new ValuesRestriction<Long>().withInclude(ids);
        return this;
    }

    public SearchNotificationParameters withIdsIncluded(Long... ids) {
        this.ids = new ValuesRestriction<Long>().withInclude(Arrays.asList(ids));
        return this;
    }

    public SearchNotificationParameters withIdsExcluded(Collection<Long> ids) {
        this.ids = new ValuesRestriction<Long>().withExclude(ids);
        return this;
    }

    public SearchNotificationParameters withIdsExcluded(Long... ids) {
        this.ids = new ValuesRestriction<Long>().withExclude(Arrays.asList(ids));
        return this;
    }
}
