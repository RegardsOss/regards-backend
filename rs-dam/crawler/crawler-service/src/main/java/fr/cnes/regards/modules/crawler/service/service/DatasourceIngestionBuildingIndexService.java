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

package fr.cnes.regards.modules.crawler.service.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.job.CrawlOneDatasourceJob;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.cnes.regards.modules.crawler.service.service.CrawlerCreatorService.managing;

/**
 * Service managing datasource ingestions on the “building” index.
 * Handles ingestion retries, alias switching, and final update
 * of building datasource ingestions once completed.
 */
@Service
public class DatasourceIngestionBuildingIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceIngestionBuildingIndexService.class);

    private final IPluginService pluginService;

    private final IDatasourceIngestionRepository dsIngestionRepos;

    private final IndexAliasResolver indexAliasResolver;

    private final DatasourceIngestionService datasourceIngestionService;

    private final IndexService indexService;

    private final JobInfoService jobInfoService;

    private final DatasourceIngestionBuildingIndexService self;

    public DatasourceIngestionBuildingIndexService(IPluginService pluginService,
                                                   IDatasourceIngestionRepository dsIngestionRepos,
                                                   IndexAliasResolver indexAliasResolver,
                                                   DatasourceIngestionService datasourceIngestionService,
                                                   IndexService indexService,
                                                   JobInfoService jobInfoService,
                                                   @Lazy DatasourceIngestionBuildingIndexService self) {
        this.pluginService = pluginService;
        this.dsIngestionRepos = dsIngestionRepos;
        this.indexAliasResolver = indexAliasResolver;
        this.datasourceIngestionService = datasourceIngestionService;
        this.indexService = indexService;
        this.jobInfoService = jobInfoService;
        this.self = self;
    }

    /**
     * Manages the “building” ingestion cycle for the current tenant.
     * If no building index exists, returns immediately (no-op).
     * If any building ingestion finished with a non-success final status, clears lastIngestDate to restart and returns.
     * If all building ingestions have completed the required second pass, promotes the building index (switches the
     * alias).
     */
    @MultitenantTransactional
    public void manageBuildingDatasourceIngestions(String tenant) {
        //If no index building, then no reindexation running, so just return
        if (indexAliasResolver.resolveBuildingIndex(tenant).isEmpty()) {
            return;
        }
        // We get all the datasource plugin configs IDs that are active
        Set<String> activePluginIds = pluginService.getPluginConfigurationsByType(IDataSourcePlugin.class)
                                                   .stream()
                                                   .filter(PluginConfiguration::isActive)
                                                   .map(PluginConfiguration::getBusinessId)
                                                   .collect(Collectors.toSet());

        List<DatasourceIngestion> allDsIngestions = dsIngestionRepos.findAll();

        // We get the datasource ingestions for building index matching the active plugin configs
        List<DatasourceIngestion> dsIngestionsBuilding = allDsIngestions.stream()
                                                                        .filter(DatasourceIngestion::isBuilding)
                                                                        .filter(dsi -> activePluginIds.contains(
                                                                            datasourceIngestionService.stripBuildingSuffix(
                                                                                dsi.getId())))
                                                                        .toList();

        List<DatasourceIngestion> dsIngestionsBuildingInactive = allDsIngestions.stream()
                                                                                .filter(DatasourceIngestion::isBuilding)
                                                                                .filter(dsi -> !activePluginIds.contains(
                                                                                    datasourceIngestionService.stripBuildingSuffix(
                                                                                        dsi.getId())))
                                                                                .toList();

        // We get the datasource ingestions for current index matching the active plugin configs
        List<DatasourceIngestion> dsIngestionsCurrent = allDsIngestions.stream()
                                                                       .filter(dsi -> !dsi.isBuilding())
                                                                       .toList();
        OffsetDateTime now = OffsetDateTime.now();

        // Case KO : at least one datasource ingestion has a non-OK final status
        if (handleNonOkFinalStatuses(dsIngestionsBuilding)) {
            return;
        }

        // Case all the ingestions have been done twice for each datasource
        Set<String> pluginsWithSecondPassDone = dsIngestionsBuilding.stream()
                                                                    .filter(dsi -> isSecondPassCompleted(dsi, now))
                                                                    .map(dsi -> datasourceIngestionService.stripBuildingSuffix(
                                                                        dsi.getId()))
                                                                    .collect(Collectors.toSet());

        // We check that all the active plugins have a corresponding datasource ingestion that has been passed twice
        Set<String> missing = new HashSet<>(activePluginIds);
        missing.removeAll(pluginsWithSecondPassDone);

        if (!missing.isEmpty()) {
            LOGGER.info("Wait before alias switch: second pass not completed on tenant {} for {} plugin(s): {}",
                        tenant,
                        missing.size(),
                        missing);
            return;
        }

        //We lock the crawling ingestion from datasources before updating the indices and the datasource ingestions
        if (managing.getAndSet(true)) {
            return;
        }
        try {
            if (indexService.updateAliasWithBuildingIndex(tenant)) {
                //We remove the old datasourceIngestions, they will be replaced by the building ones
                dsIngestionsCurrent.forEach(dsi -> datasourceIngestionService.deleteDatasourceIngestion(dsi.getId()));

                List<DatasourceIngestion> allBuildingDsi = Stream.concat(dsIngestionsBuilding.stream(),
                                                                         dsIngestionsBuildingInactive.stream())
                                                                 .toList();

                // We need to do this part after the commit of the transaction because we need to wait that there is
                // no active jobs on the old datasourceIngestions anymore and it can take too much time
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        postCommitWaitAndFinish(dsIngestionsCurrent, allBuildingDsi);
                    }
                });
            } else {
                LOGGER.error("The ElasticSearch alias has not been correctly updated for alias {}, tenant {}",
                             IndexAliasResolver.resolveAliasName(tenant),
                             tenant);
            }
        } finally {
            managing.set(false);
        }

    }

    private boolean hasActiveJob(DatasourceIngestion dsi) {
        UUID jobId = dsi.getJobId();
        if (jobId == null) {
            return false;
        }
        JobInfo jobInfo = jobInfoService.retrieveJob(jobId); // null if already purged
        return jobInfo != null && !jobInfo.getStatus().getStatus().isFinished();
    }

    /**
     * If the status of the datasourceIngestions is final but not sucecssful, we clear their lastIngestDate to restart
     * the crawling for this datasource
     *
     * @return true if there is at least one datasourceIngestion final but not OK
     */
    private boolean handleNonOkFinalStatuses(List<DatasourceIngestion> datasourceIngestions) {
        boolean hasNonOk = datasourceIngestions.stream()
                                               .anyMatch(d -> (d.getStatus().isFinal() && !d.getStatus().isSuccess()));
        if (hasNonOk) {
            // we clear lastIngestDate to restart crawling
            datasourceIngestions.stream()
                                .filter(d -> (d.getStatus().isFinal() && !d.getStatus().isSuccess()))
                                .forEach(d -> d.setLastIngestDate(null));
            dsIngestionRepos.saveAll(datasourceIngestions);
            return true;
        }
        return false;
    }

    /**
     * The datasource ingestion has been done twice if the status is a success and if nextPlannedIngestDate is not
     * set before now. The nextPlannedIngestDate is updated within the {@link CrawlOneDatasourceJob}.
     */
    private boolean isSecondPassCompleted(DatasourceIngestion dsi, OffsetDateTime now) {
        OffsetDateTime next = dsi.getNextPlannedIngestDate();
        return next != null && (next.isAfter(now)) && dsi.getStatus().isSuccess();
    }

    /**
     * Waits for the end of the residual jobs from the old datasource ingestions and then updates the new datasource
     * ingestions
     */
    private void postCommitWaitAndFinish(List<DatasourceIngestion> deletedDatasourceIngestions,
                                         List<DatasourceIngestion> buildingDatasourceIngestions) {
        boolean cleared = waitUntilNoActiveJobs(deletedDatasourceIngestions,
                                                Duration.ofMinutes(5),
                                                Duration.ofSeconds(5));
        if (!cleared) {
            LOGGER.error("Timeout waiting residual jobs");
            return;
        }
        self.updateAllBuildingDatasourceIngestions(buildingDatasourceIngestions);
    }

    /**
     * Waits (by polling) until there are no more active background jobs associated with the given
     * datasourceIngestion IDs, or until the timeout elapses.
     */
    private boolean waitUntilNoActiveJobs(List<DatasourceIngestion> datasourceIngestions,
                                          Duration timeout,
                                          Duration poll) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean anyActive = datasourceIngestions.stream().anyMatch(this::hasActiveJob);
            if (!anyActive) {
                return true;
            }
            try {
                Thread.sleep(poll.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Update all the {@link DatasourceIngestion} for the building index after reindexing completion.
     * They are not for building index anymore, so we update their id and label accordingly
     */
    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateAllBuildingDatasourceIngestions(List<DatasourceIngestion> dsIngestionsBuilding) {
        for (DatasourceIngestion datasourceIngestion : dsIngestionsBuilding) {
            DatasourceIngestion newDatasourceIngestion = datasourceIngestion.clone(datasourceIngestionService.stripBuildingSuffix(
                                                                                       datasourceIngestion.getId()),
                                                                                   datasourceIngestionService.stripBuildingSuffix(
                                                                                       datasourceIngestion.getLabel()),
                                                                                   false);
            newDatasourceIngestion.setNextPlannedIngestDate(null);
            dsIngestionRepos.save(newDatasourceIngestion);
            datasourceIngestionService.deleteDatasourceIngestion(datasourceIngestion.getId());
        }
    }

}
