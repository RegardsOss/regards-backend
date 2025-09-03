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
package fr.cnes.regards.modules.crawler.service.service.parallel;

import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionService;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionStatusService;
import fr.cnes.regards.modules.crawler.service.service.IngestionParameters;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.BulkSaveLightResult;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class manages the bulk save operations to ElasticSearch in separated threads for a specific datasource ingestion.
 * Each use of {@link #saveDataObjectAsync(List)} creates an upsert operation for each DataObject in the provided list.
 * These operations are stored in a bulk and then the method creates a new asynchronous task that sends the bulk to the ElasticSearch index, and waits for the result.
 * <p>
 * This class is created in {@link EsBulkSaveService} for each datasource ingestion and allows to save data objects asynchronously.
 */
public class EsBulkParallelSaver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsBulkParallelSaver.class);

    private final EsBulkSaveService esBulkSaveService;

    private final DatasourceIngestionService datasourceIngestionService;

    private final DatasourceIngestionStatusService datasourceIngestionStatusService;

    private final IngestionParameters ingestionParameters;

    private final DatasourceIngestion datasourceIngestion;

    /**
     * This list contains all the async tasks that were executed during the ingestion process.
     * It is used to wait for all tasks to complete and to collect the results, or collect intermediate results.
     * <p>
     * This list is ordered by insertion time. This list cleans up the done tasks when intermediate results are calculated.
     */
    private final List<EsBulkTaskInformation> allAsyncTasks = new ArrayList<>();

    /**
     * This variable holds the failure context (exception and cursor) of the bulk save operations.
     */
    private final AtomicReference<EsBulkFailureContext> failureContext = new AtomicReference<>(null);

    /**
     * This variable holds the first bulk that has been added to the current bulk save manager.
     */
    private EsBulkTaskInformation firstBulk;

    /**
     * This variable is updated regularly to retain the results of previous bulk operations.
     */
    private final BulkSaveLightResult intermediateResult = new BulkSaveLightResult();

    public EsBulkParallelSaver(IngestionParameters ingestionParameters,
                               DatasourceIngestion datasourceIngestion,
                               EsBulkSaveService esBulkSaveService,
                               DatasourceIngestionService ingesterService,
                               DatasourceIngestionStatusService datasourceIngestionStatusService) {
        this.ingestionParameters = ingestionParameters;
        this.datasourceIngestion = datasourceIngestion;
        this.esBulkSaveService = esBulkSaveService;
        this.datasourceIngestionService = ingesterService;
        this.datasourceIngestionStatusService = datasourceIngestionStatusService;
    }

    /**
     * This method checks if there are any errors in the task results.
     */
    public boolean hasErrors() {
        return failureContext.get() != null;
    }

    /**
     * This method saves a list of DataObjects asynchronously.
     */
    @SuppressWarnings("java:S2221")  // catch all to ensure set error status
    public void saveDataObjectAsync(List<DataObject> dataObjects) {
        CrawlingCursor currentCursor = datasourceIngestion.getCursor().clone();
        Future<BulkSaveResult> bulkSaveResultFuture = esBulkSaveService.submitToSaveThreadPool(() -> {
            if (hasErrors()) {
                if (currentCursor.isAfter(failureContext.get().cursor(),
                                          ingestionParameters.dsPlugin().getCrawlingCursorMode())) {
                    // if oldest error cursor is older than current cursor, stop task
                    LOGGER.warn("Skipping save for cursor {} because an error occurred on an older cursor.",
                                currentCursor.getPosition());
                    return null;
                }
                // continue execution because the cursor in error concerns one of the next cursors in the bulk.
            }
            try {
                esBulkSaveService.setTenant(ingestionParameters.tenant());
                return datasourceIngestionService.createOrUpdateDataObjects(ingestionParameters,
                                                                            datasourceIngestion.getId(),
                                                                            dataObjects);
            } catch (Exception e) {
                LOGGER.error("Error while creating or merging data objects", e);
                storeErrorIfNeeded(currentCursor, e);
                throw e;
            }
        });
        EsBulkTaskInformation esBulkTaskInformation = new EsBulkTaskInformation(currentCursor, bulkSaveResultFuture);
        allAsyncTasks.add(esBulkTaskInformation);
        if (firstBulk == null) {
            firstBulk = esBulkTaskInformation;
        } else if (allAsyncTasks.size() >= 10) {
            calculateIntermediateResults();
        }
    }

    /**
     * This method calculates the intermediate results of the bulk save operations.
     * It collects the results of the completed tasks and updates the ingestion status with the most advanced cursor.
     * If any task failed, it stores the error in the failure context.
     */
    private void calculateIntermediateResults() {
        // get done tasks and remove them from the task list
        // stop at the first task that is not done (task list is ordered by insertion time)
        // Warning : a done task can be an error
        List<EsBulkTaskInformation> tasksDone = allAsyncTasks.stream()
                                                             .takeWhile(task -> task.futureBulkSaveResult().isDone())
                                                             .toList();
        for (EsBulkTaskInformation task : tasksDone) {
            try {
                intermediateResult.append(task.futureBulkSaveResult().get());
                allAsyncTasks.remove(task);
            } catch (CancellationException | InterruptedException | ExecutionException ex) {
                // future.get() throw the task exception if any (encapsulated in ExecutionException)
                storeErrorIfNeeded(task.cursor(), ex);
                LOGGER.error("Error while waiting for future task completion", ex);
                break; // do not compute tasks that are after the first error
            }
        }
        if (!CollectionUtils.isEmpty(tasksDone) && !hasErrors()) {
            // get the most advanced cursor
            CrawlingCursor oldestCursor = tasksDone.get(tasksDone.size() - 1).cursor();
            datasourceIngestionStatusService.updateIngesterResult(datasourceIngestion.getId(),
                                                                  new IngestionResult(ingestionParameters.ingestionStart(),
                                                                                      intermediateResult.getSavedDocsCount(),
                                                                                      intermediateResult.getInErrorDocsCount(),
                                                                                      oldestCursor.getCurrentLastEntityDate(),
                                                                                      oldestCursor.getPreviousLastEntityDate(),
                                                                                      oldestCursor.getCurrentLastId(),
                                                                                      oldestCursor.getPreviousLastId()),
                                                                  false);
        }
    }

    /**
     * Thread-safe update of the failure context with the provided exception and cursor, only if cursor provided is before than stored one.
     */
    private void storeErrorIfNeeded(CrawlingCursor cursorToStore, Exception e) {
        failureContext.updateAndGet(existing -> {
            if (existing == null || cursorToStore.isBefore(existing.cursor(),
                                                           ingestionParameters.dsPlugin().getCrawlingCursorMode())) {
                return new EsBulkFailureContext(cursorToStore, e);
            } else {
                return existing;
            }
        });
    }

    /**
     * This method waits for all the async futures tasks to complete, and throws an exception if any of them failed.
     *
     * @throws FirstFindException   if the first bulk save operation failed
     * @throws NotFinishedException if a later bulk save operation failed
     */
    @SuppressWarnings("java:S1166") // No need to rethrow exception here
    public BulkSaveLightResult waitAllResultsOrThrowIfAnyFail() throws FirstFindException, NotFinishedException {
        LOGGER.info("Waiting for all results");
        // 1. loop of get() -> wait for all futures to complete, and catch unexpected exceptions to set the first error
        for (EsBulkTaskInformation task : allAsyncTasks) {
            try {
                task.futureBulkSaveResult().get();
            } catch (CancellationException | InterruptedException | ExecutionException ex) {
                // future.get() throw the task exception if any (encapsulated in ExecutionException)
                storeErrorIfNeeded(task.cursor(), ex);
                LOGGER.error("Error while waiting for future task completion", ex);
            }
        }
        // 2. throw an exception if any error occurred during the bulk save operations.
        if (hasErrors()) {
            collectSuccessResultsAndThrow();
        }
        // 3. no errors occurred, we can collect the results of all tasks
        BulkSaveLightResult bulkSaveLightResult = intermediateResult;
        for (EsBulkTaskInformation task : allAsyncTasks) {
            try {
                bulkSaveLightResult.append(task.futureBulkSaveResult().get());
            } catch (CancellationException | InterruptedException | ExecutionException ignored) {
                // do nothing, we already handled the error in the first loop
            }
        }
        return bulkSaveLightResult;
    }

    /**
     * Collect all successful cursors that are before the provided cursor on error.
     * This method is used to set partial ingestion results
     */
    @SuppressWarnings("java:S1166") // No need to rethrow exception here
    private void collectSuccessResultsAndThrow() throws FirstFindException, NotFinishedException {
        BulkSaveLightResult partialResult = intermediateResult;
        EsBulkFailureContext failure = failureContext.get();
        // loop to collect the results that are before the first error of the bulk save operations
        for (EsBulkTaskInformation task : allAsyncTasks) {
            try {
                if (!task.cursor().equals(failure.cursor()) && task.cursor()
                                                                   .isBefore(failure.cursor(),
                                                                             ingestionParameters.dsPlugin()
                                                                                                .getCrawlingCursorMode())) {
                    // if task cursor is older than the error cursor, collect the result
                    partialResult.append(task.futureBulkSaveResult().get());
                }
            } catch (CancellationException | InterruptedException | ExecutionException ignored) {
                // do nothing, we already handled the error in the first loop
            }
        }
        if (failConcernTheFirstBulk()) {
            // Error comes from the first bulk, this a first find error
            throw new FirstFindException(failure.exception(), failure.cursor());
        } else {
            // Error comes from a later bulk, this is a not finished error. A partial save result is available.
            throw new NotFinishedException(failure.exception(), partialResult, failure.cursor());
        }
    }

    /**
     * Checks if the first bulk save operation failed
     */
    private boolean failConcernTheFirstBulk() {
        if (failureContext.get() == null) {
            // No error occurred
            return false;
        } else {
            return failureContext.get().cursor().equals(firstBulk.cursor());
        }
    }
}
