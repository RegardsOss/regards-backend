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
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.Optional;

/**
 * Service dedicated to the status update of a {@link DatasourceIngestion}
 *
 * @author tguillou
 */
@Service
public class DatasourceIngestionStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceIngestionStatusService.class);

    private static final int MAX_NOTIFICATION_LENGTH = 512;

    private final IDatasourceIngestionRepository datasourceIngestionRepos;

    private final INotificationClient notifClient;

    public DatasourceIngestionStatusService(IDatasourceIngestionRepository datasourceIngestionRepos,
                                            INotificationClient notifClient) {
        this.datasourceIngestionRepos = datasourceIngestionRepos;
        this.notifClient = notifClient;
    }

    /**
     * Launch ingestion associated to the given {@link DatasourceIngestion}
     */
    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void updateIngesterResult(String dsIngestionId, IngestionResult summary, boolean crawlingCompleted) {
        Optional<DatasourceIngestion> oDsIngestion = datasourceIngestionRepos.findById(dsIngestionId);
        if (oDsIngestion.isPresent()) {
            DatasourceIngestion dsIngestion = oDsIngestion.get();
            if (crawlingCompleted) {
                // dsIngestion.stackTrace has been updated by handleMessageEvent transactional method
                if (summary.getInErrorObjectsCount() > 0) {
                    dsIngestion.setStatus(IngestionStatus.FINISHED_WITH_WARNINGS);
                } else {
                    dsIngestion.setStatus(IngestionStatus.FINISHED);
                }
            }
            dsIngestion.setSavedObjectsCount(summary.getSavedObjectsCount());
            dsIngestion.setInErrorObjectsCount(summary.getInErrorObjectsCount());
            dsIngestion.setLastIngestDate(summary.getDate());
            // To avoid redoing an ingestion in this "do...while" (must be at next call to manage)
            dsIngestion.setNextPlannedIngestDate(null);
            // To avoid redoing an ingestion from beginning in case where plugin are date optimized (or id optimized)
            if (summary.getLastEntityDate() != null) {
                dsIngestion.setLastEntityDate(summary.getLastEntityDate(), summary.getPenultimateLastEntityDate());
            }
            if (summary.getLastId() != null) {
                dsIngestion.setLastId(summary.getLastId(), summary.getPreviousLastId());
            }
            // Save ingestion status
            LOGGER.info("Calculating done : save here {}", dsIngestion.getSavedObjectsCount());
            DatasourceIngestion ds = datasourceIngestionRepos.save(dsIngestion);
            // Don't know why flush is require here, but it is.
            datasourceIngestionRepos.flush();
            if (crawlingCompleted) {
                sendNotificationSummary(ds);
            }
        } else {
            LOGGER.warn("Unable to find datasource with id {} to set indexation results", dsIngestionId);
        }
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationSummary(DatasourceIngestion dsIngestion) {
        // Send admin notification for ingestion ends if something as been done
        if ((dsIngestion.getSavedObjectsCount() != 0) || (dsIngestion.getInErrorObjectsCount() != 0)) {
            String title = String.format("%s indexation ends.", dsIngestion.getLabel());
            String stackTrace = dsIngestion.getStackTrace();
            if ((dsIngestion.getStackTrace() != null) && (stackTrace.length() > MAX_NOTIFICATION_LENGTH)) {
                stackTrace = dsIngestion.getStackTrace()
                                        .substring(0,
                                                   Math.min(dsIngestion.getStackTrace().length(),
                                                            MAX_NOTIFICATION_LENGTH)) + " ... [truncated]";
            }
            switch (dsIngestion.getStatus()) {
                case ERROR -> notifClient.notify(String.format("Indexation error. Cause : %s", stackTrace),
                                                 title,
                                                 NotificationLevel.ERROR,
                                                 DefaultRole.PROJECT_ADMIN);
                case FINISHED_WITH_WARNINGS -> notifClient.notify(String.format(
                    "Indexation ends with %s new indexed objects and %s errors.",
                    dsIngestion.getSavedObjectsCount(),
                    dsIngestion.getInErrorObjectsCount()), title, NotificationLevel.WARNING, DefaultRole.PROJECT_ADMIN);
                case NOT_FINISHED -> notifClient.notify(String.format("""
                                                                          Indexation ends with %s new indexed objects and %s errors but is not completely terminated.
                                                                                     Something went wrong concerning datasource or Elasticsearch.
                                                                          Associated datasets haven't been updated, ingestion may be manually re-scheduled
                                                                          to be launched as soon as possible or will continue at its planned date
                                                                          """,
                                                                      dsIngestion.getSavedObjectsCount(),
                                                                      dsIngestion.getInErrorObjectsCount()),
                                                        title,
                                                        NotificationLevel.WARNING,
                                                        DefaultRole.PROJECT_ADMIN);
                default -> notifClient.notify(String.format(
                    "Indexation finished. %s new objects indexed. %s objects in error.",
                    dsIngestion.getSavedObjectsCount(),
                    dsIngestion.getInErrorObjectsCount()), title, NotificationLevel.INFO, DefaultRole.PROJECT_ADMIN);
            }
        }
    }
}
