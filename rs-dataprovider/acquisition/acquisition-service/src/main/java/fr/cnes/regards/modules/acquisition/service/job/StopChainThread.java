/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    @Autowired
    private IAcquisitionProcessingService processingService;

    private final AcquisitionProcessingChain processingChain;

    private static final int TIMEOUT = 60000; // 60 seconds

    private static final int SLEEP_TIME = 1000; // 1 second

    public StopChainThread(AcquisitionProcessingChain processingChain) {
        super("Stopping chain " + processingChain.getLabel());
        this.processingChain = processingChain;
    }

    @Override
    public void run() {

        LOGGER.info("Waiting timeout set to {} seconds", TIMEOUT);

        int totalWaiting = 0;

        try {
            while ((totalWaiting < TIMEOUT)
                    && !processingService.isChainJobStoppedAndCleaned(processingChain.getId())) {
                totalWaiting += SLEEP_TIME;
                Thread.sleep(SLEEP_TIME);
                LOGGER.info("Waiting for the chain to stop since {} seconds", totalWaiting);
            }

            if (totalWaiting < TIMEOUT) {
                // Unlock chain
                processingService.unlockChain(processingChain);
            }
            // FIXME manage timeout or not!
            // FIXME add notification

        } catch (ModuleException | InterruptedException ex) {
            LOGGER.error("Processing chain clean thread failure", ex);
            // FIXME add notification
        }
    }
}
