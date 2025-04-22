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
package fr.cnes.regards.modules.crawler.service.job;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.crawler.service.service.EntityIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Job that will run {@link EntityIndexerService#updateEntityIntoEs} for the ipId in parameter
 *
 * @author Thibaud Michaudel
 **/
public class UpdateEntityIntoEsJob extends AbstractJob<Void> {

    public static final String URN_PARAMETER = "urn";

    public static final String REQUEST_ID_PARAMETER = "requestId";

    public static final String USER_TO_NOTIFY_PARAMETER = "userToNotify";

    public static final String ROLE_TO_NOTIFY_PARAMETER = "roleToNotify";

    private String datasetUrn;

    private Long requestId;

    private String userToNotify;

    private String roleToNotify;

    @Autowired
    private EntityIndexerService entityIndexerService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        datasetUrn = getValue(parameters, URN_PARAMETER);
        requestId = getValue(parameters, REQUEST_ID_PARAMETER);
        userToNotify = (String) getOptionalValue(parameters, USER_TO_NOTIFY_PARAMETER, String.class).orElse(null);
        roleToNotify = (String) getOptionalValue(parameters, ROLE_TO_NOTIFY_PARAMETER, String.class).orElse(null);
    }

    @Override
    public void run() {
        logger.info("[UPDATE ENTITY INTO ES JOB] Running job to update {}", datasetUrn);
        long start = System.currentTimeMillis();
        entityIndexerService.runEntityRequest(requestId);
        try {
            notifyDatasetUpdateBeginning();
            entityIndexerService.updateEntityIntoEs(runtimeTenantResolver.getTenant(),
                                                    UniformResourceName.fromString(datasetUrn),
                                                    OffsetDateTime.now(),
                                                    false);
            notifyDatasetUpdateDone();
            entityIndexerService.deleteEntityRequest(requestId);
        } catch (ModuleException e) {
            logger.error("Error while updating entity {}", datasetUrn);
            notifyDatasetUpdateError(e);
            throw new RsRuntimeException(e);
        }
        logger.info("[UPDATE ENTITY INTO ES JOB] Job handled for {} update in {}ms",
                    datasetUrn,
                    System.currentTimeMillis() - start);
    }

    private void notifyDatasetUpdateError(ModuleException e) {
        String message = String.format(
            "Dataset %s could not be modified after all because of an unexpected issue: \"%s\"."
            + " Please change them once again to retry.",
            datasetUrn,
            e.getMessage());
        String title = "Access right update error";
        notifyUSerOrRole(message, title, NotificationLevel.ERROR);
    }

    private void notifyDatasetUpdateBeginning() {
        String message = String.format("Dataset %s access has been modified.", datasetUrn);
        String title = "Dataset update beginning";
        notifyUSerOrRole(message, title, NotificationLevel.INFO);
    }

    private void notifyDatasetUpdateDone() {
        String message = String.format("Dataset %s access has been modified.", datasetUrn);
        String title = "Dataset update done";
        notifyUSerOrRole(message, title, NotificationLevel.INFO);
    }

    private void notifyUSerOrRole(String message, String title, NotificationLevel level) {
        if (userToNotify != null) {
            // Dataset {} has been modified. Users having group {} ....
            notificationClient.notify(message, title, level, List.of(userToNotify).toArray(new String[0]));
        } else if (roleToNotify != null) {
            // Dataset {} has been modified. Users having group {} ....
            notificationClient.notifyRoles(message,
                                           title,
                                           level,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(roleToNotify));
        }
    }
}