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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateDisseminationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.request.event.DisseminationAckEvent;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Listen to {@link DisseminationAckEvent} and create tasks to update aips
 *
 * @author Michael NGUYEN
 */
@Component
public class DisseminationAckHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<DisseminationAckEvent> {

    private final ISubscriber subscriber;

    private AIPUpdateRequestService aipUpdateRequestService;

    private AIPService aipService;

    public DisseminationAckHandler(ISubscriber subscriber,
                                   AIPUpdateRequestService aipUpdateRequestService,
                                   AIPService aipService) {
        this.subscriber = subscriber;
        this.aipUpdateRequestService = aipUpdateRequestService;
        this.aipService = aipService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DisseminationAckEvent.class, this);
    }

    @Override
    public Errors validate(DisseminationAckEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<DisseminationAckEvent> messages) {

        List<String> urns = messages.stream().map(DisseminationAckEvent::getUrn).toList();
        Collection<AIPEntity> aips = aipService.findByAipIds(urns);

        for (AIPEntity aip : aips) {
            Optional<String> label = messages.stream()
                                             .filter(message -> message.getUrn().equals(aip.getAipIdUrn().toString()))
                                             .map(DisseminationAckEvent::getRecipientLabel)
                                             .findFirst();
            if (label.isPresent()) {
                List<AbstractAIPUpdateTask> updateDisseminationTasks = Lists.newArrayList(AIPUpdateDisseminationTask.build(
                    AIPUpdateTaskType.UDPATE_DISSEMINATION,
                    AIPUpdateState.READY,
                    Lists.newArrayList(new DisseminationInfo(label.get(), null, OffsetDateTime.now()))));
                Multimap<AIPEntity, AbstractAIPUpdateTask> updateTasksByAIP = ArrayListMultimap.create();
                updateTasksByAIP.putAll(aip, updateDisseminationTasks);
                aipUpdateRequestService.create(updateTasksByAIP);
            } else {
                LOGGER.error("No recipient label found for DisseminationAckEvent with urn {}",
                             aip.getAipIdUrn().toString());
            }

        }
    }
}