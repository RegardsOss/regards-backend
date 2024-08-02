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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to send acquisition notifications
 *
 * @author Stephane Cortine
 */
@Service
@MultitenantTransactional
public class AcquisitionNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionNotificationService.class);

    private final INotificationClient notificationClient;

    private final ITemplateService templateService;

    public AcquisitionNotificationService(INotificationClient notificationClient, ITemplateService templateService) {
        this.notificationClient = notificationClient;
        this.templateService = templateService;
    }

    public void notifyExecutionBlockers(String chainLabel, List<String> executionBlockers) {
        Assert.hasLength(chainLabel, "Chain label is required");
        Assert.notNull(executionBlockers, "List of acquisition chain execution blockers is required");
        Assert.notEmpty(executionBlockers, "List of acquisition chain execution blockers must not be empty");

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("chainLabel", chainLabel);
        dataMap.put("executionBlockers", executionBlockers);
        try {
            String message = templateService.render(AcquisitionTemplateConfiguration.EXECUTION_BLOCKERS_TEMPLATE,
                                                    dataMap);
            notificationClient.notify(message,
                                      "Acquisition chain execution blockers",
                                      NotificationLevel.ERROR,
                                      MimeTypeUtils.TEXT_HTML,
                                      DefaultRole.PROJECT_ADMIN,
                                      DefaultRole.EXPLOIT);
        } catch (TemplateException e) {
            LOGGER.error("Unable to notify execution blockers for acquisition chain", e);
        }
    }

    public void notifyInvalidAcquisitionFile(List<AcquisitionFile> invalidFiles) {
        Assert.notNull(invalidFiles, "List of acquisition invalid files is required");
        Assert.notEmpty(invalidFiles, "List of acquisition invalid files must not be empty");

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("invalidFiles", invalidFiles);
        try {
            String message = templateService.render(AcquisitionTemplateConfiguration.ACQUISITION_INVALID_FILES_TEMPLATE,
                                                    dataMap);
            notificationClient.notify(message,
                                      "Acquisition invalid files report",
                                      NotificationLevel.WARNING,
                                      MimeTypeUtils.TEXT_HTML,
                                      DefaultRole.PROJECT_ADMIN,
                                      DefaultRole.EXPLOIT);
        } catch (TemplateException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
