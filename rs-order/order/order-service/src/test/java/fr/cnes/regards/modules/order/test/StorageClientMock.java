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
package fr.cnes.regards.modules.order.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;

public class StorageClientMock implements IStorageClient {

    @Autowired
    private final IStorageFileListener listener;

    private boolean isAvailable = true;

    public StorageClientMock(IStorageFileListener listener, boolean isAvailable) {
        super();
        this.listener = listener;
        this.isAvailable = isAvailable;
    }

    @Override
    public RequestInfo store(FileStorageRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> store(Collection<FileStorageRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeRetry(RequestInfo requestInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void storeRetry(Collection<String> owners) {
        // TODO Auto-generated method stub

    }

    @Override
    public void availabilityRetry(RequestInfo requestInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public RequestInfo reference(FileReferenceRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> reference(Collection<FileReferenceRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestInfo delete(FileDeletionRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> delete(Collection<FileDeletionRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestInfo copy(FileCopyRequestDTO file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> copy(Collection<FileCopyRequestDTO> files) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RequestInfo> makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        Collection<RequestInfo> list = new ArrayList<RequestInfo>();
        RequestInfo ri = RequestInfo.build();
        for (String c : checksums) {
            if (!isAvailable) {
                listener.onFileNotAvailable(c, Sets.newHashSet(ri), "");
            } else {
                listener.onFileAvailable(c, Sets.newHashSet(ri));
            }
        }
        list.add(ri);
        return list;
    }

}
