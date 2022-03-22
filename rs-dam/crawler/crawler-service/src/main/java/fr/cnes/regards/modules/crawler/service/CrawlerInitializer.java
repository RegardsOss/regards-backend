/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Crawler initializer.
 * This component is used to launch crawler services as daemons.
 * @author oroussel
 */
@Component
public class CrawlerInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerInitializer.class);

    @Autowired
    private ICrawlerService datasetCrawlerService;

    @Autowired
    private ICrawlerAndIngesterService crawlerAndIngesterService;

    @Autowired
    private IEsRepository repository;

    private static ExecutorService singlePool = Executors.newSingleThreadExecutor();

    private final AtomicBoolean elasticSearchUpgradeDone = new AtomicBoolean(false);

    @EventListener
    public void onApplicationEvent(ModelJsonReadyEvent event) {
        LOGGER.info("Running dataset crawler .... ");
        datasetCrawlerService.crawl();
        LOGGER.info("Dataset crawler started.");
    }

    @EventListener
    public void onApplicationEvent2(ModelJsonReadyEvent event) {
        LOGGER.info("Running standard crawler .... ");
        crawlerAndIngesterService.crawl();
        LOGGER.info("Standard crawler started.");
    }
}
