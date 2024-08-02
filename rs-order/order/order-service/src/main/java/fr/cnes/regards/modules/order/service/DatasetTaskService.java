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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.order.dao.IDatasetTaskRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class DatasetTaskService implements IDatasetTaskService {

    @Autowired
    private IDatasetTaskRepository repos;

    @Override
    public DatasetTask loadSimple(Long datasetId) throws EntityNotFoundException {
        Optional<DatasetTask> task = repos.findById(datasetId);
        return task.orElseThrow(() -> new EntityNotFoundException(datasetId, DatasetTask.class));
    }

    @Override
    public DatasetTask loadComplete(Long datasetId) {
        return repos.findCompleteById(datasetId);
    }

    @Override
    public Page<OrderDataFile> loadDataFiles(Long datasetId, Pageable pageable) {
        DatasetTask dsTask = repos.findCompleteById(datasetId);
        int cpt = 0;
        List<OrderDataFile> dataFiles = new ArrayList<>();
        for (FilesTask filesTask : dsTask.getReliantTasks()) {
            // Sort by filename before managing pagination
            List<OrderDataFile> sortedDataFiles = new ArrayList<>(filesTask.getFiles());
            sortedDataFiles.sort(Comparator.comparing(OrderDataFile::getFilename));
            for (OrderDataFile dataFile : filesTask.getFiles()) {
                if (cpt >= pageable.getOffset() && cpt < pageable.getOffset() + pageable.getPageSize()) {
                    dataFiles.add(dataFile);
                }
                cpt++;
            }
        }
        return new PageImpl<>(dataFiles, pageable, cpt);
    }

}
