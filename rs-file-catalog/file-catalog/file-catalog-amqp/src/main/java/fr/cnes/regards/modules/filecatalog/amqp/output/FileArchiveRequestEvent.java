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
package fr.cnes.regards.modules.filecatalog.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.filecatalog.dto.FileArchiveRequestDto;

/**
 * Event sent from file-catalog to file-packager in order to request the packaging of a small file
 *
 * @author Thibaud Michaudel
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FileArchiveRequestEvent extends FileArchiveRequestDto implements ISubscribable {

    public FileArchiveRequestEvent(long storageRequestId,
                                   String storage,
                                   String checksum,
                                   String fileName,
                                   String currentFileParentPath,
                                   String finalArchiveParentUrl,
                                   long fileSize) {
        super(storageRequestId, storage, checksum, fileName, currentFileParentPath, finalArchiveParentUrl, fileSize);
    }
}
