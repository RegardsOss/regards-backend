/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.crawler.service.job;

import fr.cnes.regards.framework.module.rest.exception.InactiveDatasourceException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionService;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A job to crawl a single datasource.
 * Job is passed to error only for unexpected errors (like {@link NullPointerException}. Classical nominal errors like {@link InactiveDatasourceException},
 * or {@link fr.cnes.regards.modules.crawler.service.exception.EsBulkException} don't lead to
 * an error in the job, but to a change of the datasource status.
 *
 * @author tguillou
 */
public class CrawlOneDatasourceJob extends AbstractJob<Void> {

    private static final String DATASOURCE_ID_PARAMETER = "datasourceId";

    @Autowired
    private DatasourceIngestionService datasourceIngesterService;

    private String datasourceId;

    public static Set<JobParameter> buildJobParameters(String datasourceId) {
        Set<JobParameter> parameters = new HashSet<>();
        parameters.add(new JobParameter(DATASOURCE_ID_PARAMETER, datasourceId));
        return parameters;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        datasourceId = getValue(parameters, DATASOURCE_ID_PARAMETER);
    }

    @Override
    @SuppressWarnings("java:S1181")    // Catch throwable to set ingestion to error status
    public void run() {
        logger.debug("[CRAWL ONE DATASOURCE JOB] Running crawler job on datasource {}", datasourceId);
        long start = System.currentTimeMillis();
        try {
            crawlDatasource(datasourceId);
        } catch (Throwable e) {
            setDatasourceIngestInError(datasourceId, e);
            logger.error("[CRAWL ONE DATASOURCE JOB] An error occurred during the crawling for datasource {}",
                         datasourceId,
                         e);
            throw new JobRuntimeException(e);
        }
        logger.debug("[CRAWL ONE DATASOURCE JOB] Job handled for the datasource crawl with id {} in {}ms",
                     datasourceId,
                     System.currentTimeMillis() - start);
    }

    private void crawlDatasource(String datasourceId) {
        Optional<IngestionResult> summary = ingest(datasourceId);
        summary.ifPresent(ingestionResult -> datasourceIngesterService.updateIngesterResult(datasourceId,
                                                                                            ingestionResult));
    }

    private Optional<IngestionResult> ingest(String dsId) {
        Optional<IngestionResult> summary = Optional.empty();
        try {
            summary = datasourceIngesterService.ingest(dsId);
        } catch (InactiveDatasourceException ide) {
            logger.warn(ide.getMessage(), ide);
            datasourceIngesterService.setInactive(dsId, ide.getMessage());
        } catch (ModuleException | FirstFindException e) {
            // ModuleException can only be thrown before we start reading the datasource so it's simply an error
            setDatasourceIngestInError(dsId, e);
        } catch (NotFinishedException nfe) {
            logger.error(nfe.getMessage(), nfe);
            datasourceIngesterService.setNotFinished(dsId, nfe);
        }
        return summary;
    }

    private void setDatasourceIngestInError(String dsId, Throwable e) {
        logger.error("Datasource ingestion error : {}", e.getMessage(), e);
        CrawlingCursor cursorToSet = null;
        if (e instanceof FirstFindException firstFindException) {
            cursorToSet = firstFindException.getErrorCursor();
        }
        if (e instanceof NotFinishedException notFinishedException) {
            cursorToSet = notFinishedException.getErrorCursor();
        }
        try (StringWriter sw = new StringWriter()) {
            e.printStackTrace(new PrintWriter(sw));
            datasourceIngesterService.setError(dsId, sw.toString(), cursorToSet);
        } catch (IOException e1) {
            logger.error(e1.getMessage(), e1);
            datasourceIngesterService.setError(dsId, e.getMessage(), cursorToSet);
        }
    }

}
