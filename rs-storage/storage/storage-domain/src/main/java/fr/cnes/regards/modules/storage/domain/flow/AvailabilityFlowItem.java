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
package fr.cnes.regards.modules.storage.domain.flow;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;

/**
 * Flow message to request file(s) to be available for download.<br/>
 * Files stored with an ONLINE {@link IStorageLocation} plugin are immediately available <br/>
 * Files stored with an NEARLINE {@link IStorageLocation} plugin needs to be retrieved in cache before being available<br/>
 * Files not stored (only reference) or OFFLINE cannot be available <br/>
 * <br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class AvailabilityFlowItem implements ISubscribable {

    public static final int MAX_REQUEST_PER_GROUP = 1000;

    /**
     * Checksums of files to make available for download
     */
    private final Set<String> checksums = Sets.newHashSet();

    /**
     * Expiration date for files availability
     */
    private OffsetDateTime expirationDate;

    /**
     * Request business identifier
     */
    private String groupId;

    /**
     * Build a availability request item.
     * @param checksums
     * @param expirationDate
     * @param groupId
     * @return {@link AvailabilityFlowItem}
     */
    public static AvailabilityFlowItem build(Collection<String> checksums, OffsetDateTime expirationDate,
            String groupId) {
        AvailabilityFlowItem item = new AvailabilityFlowItem();
        Assert.notNull(checksums, "Checksums is mandatory");
        Assert.notEmpty(checksums, "Checksums is mandatory");
        Assert.notNull(expirationDate, "Expiration date is mandatory");
        Assert.notNull(groupId, "groupId is mandatory");
        item.checksums.addAll(checksums);
        item.expirationDate = expirationDate;
        item.groupId = groupId;
        return item;
    }

    public Set<String> getChecksums() {
        return checksums;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public String getGroupId() {
        return groupId;
    }

}
