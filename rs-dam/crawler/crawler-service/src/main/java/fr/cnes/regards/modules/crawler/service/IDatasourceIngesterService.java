package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.framework.module.rest.exception.InactiveDatasourceException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;

import java.util.List;
import java.util.Optional;

/**
 * @author oroussel
 */
public interface IDatasourceIngesterService {

    /**
     * Ingest provided datasource (from plugin configuration) data objects into Elasticsearch
     *
     * @param dsi datasource ingestion status object
     * @return a summary containing the count of DataObjects ingested from given datasource and the ingestion date
     * @throws ModuleException             Can be thrown while getting datasource implementation
     * @throws NotFinishedException        Some issue was encountered while reading all pages from datasource but at
     *                                     least first read has been successfully done
     * @throws FirstFindException          Some issue was encountered while reading datasource first page
     * @throws InactiveDatasourceException Datasource is not active
     */
    Optional<IngestionResult> ingest(String dsi)
        throws ModuleException, NotFinishedException, FirstFindException, InactiveDatasourceException;

    /**
     * Retrieve all {@link DatasourceIngestion}
     */
    List<DatasourceIngestion> getDatasourceIngestions();

    /**
     * Delete given {@link DatasourceIngestion}
     *
     * @param id DatasourceIngestion id
     */
    void deleteDatasourceIngestion(String id);

    /**
     * Schedule datasource ingestion to be executed as soon as possible
     */
    void scheduleNowDatasourceIngestion(String id);
}
