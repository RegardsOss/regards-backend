package fr.cnes.regards.modules.crawler.service.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Service for Dataset.
 *
 * @author oroussel
 */
@Service
public class DatasetAttributeModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetAttributeModelService.class);

    private final IDatasetService datasetService;

    private final IDatasetRepository datasetRepository;

    private final IRuntimeTenantResolver tenantResolver;

    private final EntityIndexerService entityIndexerService;

    @Autowired
    public DatasetAttributeModelService(IDatasetService datasetService,
                                        IDatasetRepository datasetRepository,
                                        IRuntimeTenantResolver tenantResolver,
                                        EntityIndexerService entityIndexerService) {
        this.datasetService = datasetService;
        this.datasetRepository = datasetRepository;
        this.tenantResolver = tenantResolver;
        this.entityIndexerService = entityIndexerService;
    }

    public void computeAttributeModel(ModelAttrAssoc modelAttrAssoc) {
        // Only recompute if a plugin conf is set (a priori if a plugin confis removed it is to be changed soon)
        if (modelAttrAssoc.getComputationConf() != null) {
            Set<Dataset> datasets = datasetService.findAllByModel(modelAttrAssoc.getModel().getId());
            for (Dataset dataset : datasets) {
                try {
                    datasetRepository.save(dataset);
                    entityIndexerService.updateEntityIntoEs(tenantResolver.getTenant(),
                                                            dataset.getIpId(),
                                                            OffsetDateTime.now(),
                                                            true);
                } catch (ModuleException e) {
                    LOGGER.error("Cannot update dataset", e);
                }
            }
        }
    }
}