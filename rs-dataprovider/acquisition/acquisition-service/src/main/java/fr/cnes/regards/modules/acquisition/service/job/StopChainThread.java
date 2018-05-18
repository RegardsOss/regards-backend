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
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * Cleaning thread
 *
 * @author Marc Sordi
 *
 */
public class StopChainThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopChainThread.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

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

        try {
            runtimeTenantResolver.forceTenant(tenant);

            LOGGER.info("Asking for processing chain jobs to stop");
            processingService.stopChainJobs(processingChainId);

            LOGGER.info("Waiting timeout set to {} seconds", TIMEOUT);
            while ((totalWaiting < TIMEOUT) && !processingService.isChainJobStoppedAndCleaned(processingChainId)) {
                totalWaiting += SLEEP_TIME;
                Thread.sleep(SLEEP_TIME);
                LOGGER.info("Waiting for the chain to stop since {} seconds", totalWaiting);
            }

            if (totalWaiting < TIMEOUT) {
                // Unlock chain
                processingService.unlockChain(processingChainId);
            }
            // FIXME manage timeout or not!
            // FIXME add notification

        } catch (ModuleException | InterruptedException ex) {
            LOGGER.error("Processing chain clean thread failure", ex);
            // FIXME add notification
        }
    }
}
