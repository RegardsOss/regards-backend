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
 * Scheduler to handle created {@link }
 *
 * @author Iliana Ghazali
 */

@Component
@Profile("!noschedule")
@EnableScheduling
public class TemporaryToponymsCleanScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(TemporaryToponymsCleanScheduler.class);



    @Autowired
    private TemporaryToponymsCleanService cleanService;

    private final static String DEFAULT_INITIAL_DELAY = "10000";
    private final static String DEFAULT_SCHEDULING_DELAY = "86400000";

    /**
     * Periodically check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every hour.
     */
    @Scheduled(initialDelayString = "${regards.toponyms.temporary.cleanup.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.toponyms.temporary.cleanup.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void cleanTemporaryToponyms() {
            LOGGER.info("[CLEAN TEMPORARY TOPONYMS JOB] - Scanning the number of temporary toponyms to delete");
            int nbDeleted = cleanService.clean();
            LOGGER.info("[CLEAN TEMPORARY TOPONYMS JOB] - Job handled in {}ms. {} temporary toponyms deleted.", System.currentTimeMillis(), nbDeleted);
        }
    }

