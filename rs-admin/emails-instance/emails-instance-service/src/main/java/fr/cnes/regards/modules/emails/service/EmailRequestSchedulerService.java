/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.emails.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler service for the email request service
 *
 * @author Stephane Cortine
 */
@Service
@EnableScheduling
public class EmailRequestSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRequestSchedulerService.class);

    private final static String DEFAULT_INITIAL_DELAY = "20000";

    private final static String DEFAULT_SCHEDULING_DELAY = "5000";

    private final EmailRequestService emailRequestService;

    public EmailRequestSchedulerService(EmailRequestService emailRequestService) {
        this.emailRequestService = emailRequestService;
    }

    @Scheduled(initialDelayString = "${regards.send.email.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.send.email.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleUpdateRequests() {
        long start = System.currentTimeMillis();
        LOGGER.info("[SEND EMAIL SCHEDULER] - Scanning email request(s) to send");
        emailRequestService.sendEmail();
        LOGGER.info("[SEND EMAIL SCHEDULER] - Handled in {}ms.", System.currentTimeMillis() - start);
    }
}