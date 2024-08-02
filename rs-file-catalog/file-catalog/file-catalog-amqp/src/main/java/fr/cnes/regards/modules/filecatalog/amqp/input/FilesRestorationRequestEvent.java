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
package fr.cnes.regards.modules.filecatalog.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.dto.files.FilesRestorationRequestDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Event to request file(s) to be available for download.<br/>
 * Files stored with an ONLINE IStorageLocation plugin are immediately available <br/>
 * Files stored with an NEARLINE IStorageLocation plugin needs to be retrieved in cache before being available<br/>
 * Files not stored (only reference) or OFFLINE cannot be available <br/>
 * <br/>
 * See FileRequestsGroupEvent for asynchronous responses when request is finished.<br/>
 * See FileReferenceEvent for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FilesRestorationRequestEvent extends FilesRestorationRequestDto implements ISubscribable {

    public static final int MAX_REQUEST_PER_GROUP = 1000;

    public FilesRestorationRequestEvent() {
        super();
    }

    public FilesRestorationRequestEvent(Set<String> checksums, int availabilityHours, String groupId) {
        super(availabilityHours, groupId, checksums);
    }

    public FilesRestorationRequestEvent(Collection<String> checksums, int availabilityHours, String groupId) {
        super(availabilityHours, groupId, new HashSet<>(checksums));
    }
}
