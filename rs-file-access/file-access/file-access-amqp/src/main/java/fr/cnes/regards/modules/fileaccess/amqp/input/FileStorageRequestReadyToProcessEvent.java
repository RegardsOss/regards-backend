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
package fr.cnes.regards.modules.fileaccess.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageRequestReadyToProcessDto;

/**
 * Event to request a new file storage to the file access microservice.<br/>
 * This request will be processed once this event is received without further filtering.
 *
 * @author Thibaud Michaudel
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FileStorageRequestReadyToProcessEvent extends FileStorageRequestReadyToProcessDto
    implements ISubscribable {

    public FileStorageRequestReadyToProcessEvent(Long requestId,
                                                 String checksum,
                                                 String algorithm,
                                                 String originUrl,
                                                 String storage,
                                                 String subDirectory,
                                                 String owner,
                                                 String session,
                                                 FileStorageMetaInfoDto metadata) {
        super(requestId, checksum, algorithm, originUrl, storage, subDirectory, owner, session, metadata);
    }
}
