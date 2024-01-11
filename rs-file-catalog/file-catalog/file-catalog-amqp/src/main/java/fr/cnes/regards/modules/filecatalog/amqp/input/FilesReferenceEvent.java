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
package fr.cnes.regards.modules.filecatalog.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dto.files.FilesReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event to request a new file reference.<br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FilesReferenceEvent extends FilesReferenceDto implements ISubscribable {

    /**
     * Maximum number of Request per event
     */
    public static final int MAX_REQUEST_PER_GROUP = 100;

    public FilesReferenceEvent() {
        super();
    }

    public FilesReferenceEvent(Set<FileReferenceRequestDto> files, String groupId) {
        super(groupId, files);
    }

    public FilesReferenceEvent(Collection<FileReferenceRequestDto> files, String groupId) {
        super(groupId, new HashSet<>(files));
    }

    public FilesReferenceEvent(FileReferenceRequestDto file, String groupId) {
        super(groupId, Stream.of(file).collect(Collectors.toSet()));
    }

    /**
     *
     */
    public Errors validate(Validator validator) {
        Errors errors;
        if (this.getFiles().size() > MAX_REQUEST_PER_GROUP) {
            errors = new MapBindingResult(new HashMap<>(), this.getClass().getName());
            errors.reject("FileReferenceRequests",
                          String.format("Number of reference requests (%d) for group %s exceeds maximum limit of %d",
                                        this.getFiles().size(),
                                        this.getGroupId(),
                                        MAX_REQUEST_PER_GROUP));
        } else {
            errors = new MapBindingResult(new HashMap<>(), FileReferenceRequestDto.class.getName());
            for (FileReferenceRequestDto file : this.getFiles()) {
                validator.validate(file, errors);
            }
        }
        return errors;
    }

}
