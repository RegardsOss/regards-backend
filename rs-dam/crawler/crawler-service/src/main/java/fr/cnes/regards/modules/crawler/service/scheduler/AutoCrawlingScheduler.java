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
package fr.cnes.regards.modules.crawler.service.scheduler;

import fr.cnes.regards.modules.crawler.service.service.IngesterService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that will periodically launch the crawling for all datasource of all tenants.
 * <p>
 * By default, launched 1 mn after last one. BUT crawling is also executed each time a datasource is created
 * Initial delay of 5 mn to avoid being launched too soon.
 *
 * @author tguillou
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class AutoCrawlingScheduler {

    private final IngesterService ingesterService;

    public AutoCrawlingScheduler(IngesterService ingesterService) {
        this.ingesterService = ingesterService;
    }

    @Scheduled(initialDelayString = "${regards.ingester.rate.init.ms:300000}",
               fixedDelayString = "${regards.ingester.rate.ms:60000}")
    public void manageAllIngestion() {
        ingesterService.manageCrawlingForAllTenants();
    }
}