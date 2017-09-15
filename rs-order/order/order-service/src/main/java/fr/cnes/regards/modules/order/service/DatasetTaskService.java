package fr.cnes.regards.modules.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.order.dao.IDatasetTaskRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;

/**
 * @author oroussel
 */
@Service
public class DatasetTaskService implements IDatasetTaskService {
    @Autowired
    private IDatasetTaskRepository repos;

    @Override
    public DatasetTask loadSimple(Long datasetId) {
        return repos.findOne(datasetId);
    }

    @Override
    public DatasetTask loadComplete(Long datasetId) {
        return repos.findCompleteById(datasetId);
    }
}
