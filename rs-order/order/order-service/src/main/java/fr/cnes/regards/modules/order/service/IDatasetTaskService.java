package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.order.domain.DatasetTask;

/**
 * @author oroussel
 */
public interface IDatasetTaskService {
    DatasetTask loadSimple(Long datasetId);

    DatasetTask loadComplete(Long datasetId);
}
