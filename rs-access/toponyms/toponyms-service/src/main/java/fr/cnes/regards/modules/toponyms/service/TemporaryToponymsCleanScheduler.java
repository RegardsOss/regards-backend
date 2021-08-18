/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.toponyms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler to delete out-dated not visible toponyms
 *
 * @author Iliana Ghazali
 */

@Component
@Profile("!noscheduler")
@EnableScheduling
public class TemporaryToponymsCleanScheduler {

    @Autowired
    private TemporaryToponymsCleanService cleanService;

    public static final Logger LOGGER = LoggerFactory.getLogger(TemporaryToponymsCleanScheduler.class);

    private final static String DEFAULT_INITIAL_DELAY = "10000";

    private final static String DEFAULT_SCHEDULING_DELAY = "86400000"; // once a day

    /**
     * Periodically check the expiration dates of not visible toponyms and eventually delete out-dated ones
     * Default : scheduled to be run every hour.
     */
    @Scheduled(initialDelayString = "${regards.toponyms.temporary.cleanup.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.toponyms.temporary.cleanup.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void cleanTemporaryToponyms() {
        long start = System.currentTimeMillis();
        LOGGER.info("[CLEAN TEMPORARY TOPONYMS SCHEDULER] - Scanning the number of temporary toponyms to delete");
        int nbDeleted = cleanService.clean();
        LOGGER.info("[CLEAN TEMPORARY TOPONYMS SCHEDULER] - {} Temporary toponyms deleted. Handled in {}ms.", nbDeleted, System.currentTimeMillis() - start);
    }
}

