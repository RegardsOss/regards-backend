/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.flow;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;

/**
 * @author Sébastien Binda
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FileReferenceFlowItem implements ISubscribable {

    private final Set<FileReferenceRequestDTO> files = Sets.newHashSet();

    private String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Set<FileReferenceRequestDTO> getFiles() {
        return files;
    }

    public static FileReferenceFlowItem build(FileReferenceRequestDTO file, String requestId) {
        FileReferenceFlowItem item = new FileReferenceFlowItem();
        item.files.add(file);
        item.requestId = requestId;
        return item;
    }

    public static FileReferenceFlowItem build(Collection<FileReferenceRequestDTO> files, String requestId) {
        FileReferenceFlowItem item = new FileReferenceFlowItem();
        item.files.addAll(files);
        item.requestId = requestId;
        return item;
    }

}
