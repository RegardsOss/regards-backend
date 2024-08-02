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

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service useful to create subtask which add dataset attached files to an order
 *
 * @author Thomas GUILLOU
 **/
@Service
@MultitenantTransactional
public class OrderAttachmentDataSetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderAttachmentDataSetService.class);

    private final IDatasetClient datasetClient;

    private final OrderHelperService orderHelperService;

    private final OrderJobService orderJobService;

    public OrderAttachmentDataSetService(IDatasetClient datasetClient,
                                         OrderHelperService orderHelperService,
                                         OrderJobService orderJobService) {
        this.datasetClient = datasetClient;
        this.orderHelperService = orderHelperService;
        this.orderJobService = orderJobService;
    }

    /**
     * Create required sub-orders containing dataset attached files. Do nothing if no files attached.
     */
    public void createSubOrderOfDataSetFiles(Order order,
                                             String owner,
                                             String role,
                                             DatasetTask dsTask,
                                             BasketDatasetSelection dsSel,
                                             int subOrderDuration) throws ModuleException {
        Set<OrderDataFile> storageBucket = new HashSet<>();
        Set<OrderDataFile> externalBucket = new HashSet<>();
        fillBucketsWithDataSetFiles(order, dsSel, storageBucket, externalBucket);
        if (!storageBucket.isEmpty()) {
            int priority = orderJobService.computePriority(owner, role);
            orderHelperService.createStorageSubOrderAndStoreDataFiles(dsTask,
                                                                      storageBucket,
                                                                      order,
                                                                      subOrderDuration,
                                                                      role,
                                                                      priority);
        }
        if (!externalBucket.isEmpty()) {
            orderHelperService.createExternalSubOrder(dsTask, externalBucket, order);
        }
    }

    /**
     * Fill two buckets <b>storageBucket</b> and <b>externalBucket</b> given in params with data set files.
     * These buckets must not be null<br>
     * An orderDataFile is created for each data set file orderable.
     * Dataset files can be stored :
     * <ul>
     *     <li>on storage service</li>
     *     <li>on dam service</li>
     *     <li>externally</li>
     * </ul>
     * Dam stored files are considered as externally, and so there is no availability verification.
     */
    public void fillBucketsWithDataSetFiles(Order order,
                                            BasketDatasetSelection dsSel,
                                            Set<OrderDataFile> storageBucket,
                                            Set<OrderDataFile> externalBucket) throws ModuleException {
        Dataset dataset = retrieveDataSetByRest(dsSel.getDatasetIpid());
        if (dataset != null) {
            Set<OrderDataFile> bucketFiles = createOrderDataFilesOf(order, dataset);
            // separate external+dam files and storages files
            Map<Boolean, List<OrderDataFile>> buckets = bucketFiles.stream()
                                                                   .collect(Collectors.groupingBy(this::isExternalOrDamFile));
            List<OrderDataFile> filesStoredInDamOrExtern = buckets.get(true);
            List<OrderDataFile> filesStoredInStorage = buckets.get(false);
            if (filesStoredInDamOrExtern != null) {
                // consider dam files as extern file
                externalBucket.addAll(filesStoredInDamOrExtern);
            }
            if (filesStoredInStorage != null) {
                storageBucket.addAll(filesStoredInStorage);
            }
        }
    }

    private boolean isExternalOrDamFile(OrderDataFile file) {
        return file.urlIsFromDam() || file.isReference();
    }

    /**
     * Create orderDataFiles from dataset attached files. Only orderable dataFile are treated.
     */
    private Set<OrderDataFile> createOrderDataFilesOf(Order order, Dataset dataset) {
        DatasetFeature feature = dataset.getFeature();
        return feature.getFiles()
                      .values()
                      .stream()
                      .filter(orderHelperService::isDataFileOrderable)
                      .map(file -> OrderDataFile.createAvailable(file,
                                                                 order.getId(),
                                                                 feature.getId(),
                                                                 feature.getProviderId(),
                                                                 feature.getVersion()))
                      .collect(Collectors.toSet());
    }

    private Dataset retrieveDataSetByRest(String datasetIpid) throws ModuleException {
        try {
            FeignSecurityManager.asSystem();
            return ResponseEntityUtils.extractBodyOrNull(datasetClient.retrieveDataset(datasetIpid));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error("Cannot retrieve dataset ", e);
            throw new ModuleException(e.getMessage());
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
