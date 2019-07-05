/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * Cleaning thread
 *
 * @author Marc Sordi
 *
 */
public class StopChainThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopChainThread.class);

    private static final String NOTIFICATION_TITLE = "Acquisition processing chain stopped";

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationClient notificationClient;

    private final Long processingChainId;

    private final String tenant;

    private static final int TIMEOUT = 60000; // 60 seconds

    private static final int SLEEP_TIME = 1000; // 1 second

    public StopChainThread(String tenant, Long processingChainId) {
        super("Stopping chain " + processingChainId);
        this.processingChainId = processingChainId;
        this.tenant = tenant;
    }

    @Override
    public void run() {

        int totalWaiting = 0;
        AcquisitionProcessingChain processingChain = null;

        try {
            runtimeTenantResolver.forceTenant(tenant);

            processingChain = processingService.getChain(processingChainId);

            LOGGER.info("Asking for processing chain jobs to stop");
            processingService.stopChainJobs(processingChainId);

            LOGGER.info("Waiting timeout set to {} milliseconds", TIMEOUT);

            boolean isStoppedAndCleaned = processingService.isChainJobStoppedAndCleaned(processingChainId);
            while (totalWaiting < TIMEOUT && !isStoppedAndCleaned) {
                totalWaiting += SLEEP_TIME;
                Thread.sleep(SLEEP_TIME);
                LOGGER.info("Waiting for the chain to stop since {} milliseconds", totalWaiting);
                isStoppedAndCleaned = processingService.isChainJobStoppedAndCleaned(processingChainId);
            }

            if (isStoppedAndCleaned) {
                notificationClient
                        .notify(String.format("Acquisition processing chain \"%s\" was properly stopped and cleaned.",
                                              processingChain.getLabel()),
                                NOTIFICATION_TITLE, NotificationLevel.INFO, DefaultRole.ADMIN);
                // Unlock chain
                processingService.unlockChain(processingChainId);
            } else {
                notificationClient.notify(String
                        .format("Acquisition processing chain \"%s\" is not yet stopped and cleaned. You have to retry stopping the chain before restarting properly!",
                                processingChain.getLabel()), NOTIFICATION_TITLE, NotificationLevel.ERROR,
                                          DefaultRole.ADMIN);
            }

        } catch (ModuleException | InterruptedException ex) {
            LOGGER.error("Processing chain clean thread failure", ex);
            String processingNameOrId;
            if (processingChain != null) {
                processingNameOrId = processingChain.getLabel();
            } else {
                processingNameOrId = String.valueOf(processingChainId);
            }
            String message = String
                    .format("Acquisition processing chain \"%s\" is not yet stopped and cleaned. An exception was thrown during stop process (%s).You have to retry stopping the chain before restarting properly!",
                            processingNameOrId, ex.getMessage());
            notificationClient.notify(message, NOTIFICATION_TITLE, NotificationLevel.ERROR, DefaultRole.ADMIN);
        }
    }
}
