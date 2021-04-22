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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class deals with events sent by rs-processing when the rights of usage on a process
 * have changed. This allows to clean up datasets selections which use a process that
 * would not be usable anymore on the dataset.
 *
 * @author Guillaume Andrieu
 *
 */
@Component
public class RightsPluginConfigurationEventHandler implements IRightsPluginConfigurationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RightsPluginConfigurationEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ISubscriber subscriber;

    private final IBasketDatasetSelectionRepository dsSelRepo;

    @Autowired
    public RightsPluginConfigurationEventHandler(IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber,
            IBasketDatasetSelectionRepository dsSelRepo) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.dsSelRepo = dsSelRepo;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(RightsPluginConfigurationEvent.class, this);
    }

    @Override
    public void handle(String tenant, RightsPluginConfigurationEvent evt) {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Received RightsPluginConfigurationEvent: {}", evt);
        switch (evt.getType()) {
            case DELETE: {
                handleDelete(evt.getBefore());
                break;
            }
            case UPDATE: {
                handleUpdate(evt.getAfter());
                break;
            }
            case CREATE:
                // Nothing to do
                break;
            default:
                LOGGER.error("Unknown RightsPluginConfigurationEvent type: {}", evt.getType());
                break;
        }
    }

    @MultitenantTransactional
    private void handleUpdate(ProcessPluginConfigurationRightsDTO after) {
        String processBusinessId = after.getPluginConfiguration().getBusinessId();
        io.vavr.collection.List<String> allowedDatasets = after.getRights().getDatasets();
        List<BasketDatasetSelection> dsSels = dsSelRepo.findByProcessBusinessId(processBusinessId);
        List<BasketDatasetSelection> modified = Stream.ofAll(dsSels).flatMap(dsSel -> {
            if (allowedDatasets.contains(dsSel.getDatasetIpid())) {
                return Option.none();
            } else {
                LOGGER.info("BasketDatasetSelection {} not applicable to process {} anymore", dsSel.getId(),
                            processBusinessId);
                dsSel.setProcessDatasetDescription(null);
                return Option.of(dsSel);
            }
        }).asJava();
        if (!modified.isEmpty()) {
            dsSelRepo.saveAll(modified);
        }
    }

    @MultitenantTransactional
    private void handleDelete(ProcessPluginConfigurationRightsDTO before) {
        // If a basket selection references the corresponding process, remove it from the selection
        String processBusinessId = before.getPluginConfiguration().getBusinessId();
        List<BasketDatasetSelection> dsSels = dsSelRepo.findByProcessBusinessId(processBusinessId);
        if (!dsSels.isEmpty()) {
            dsSels.forEach(dsSel -> {
                LOGGER.info("BasketDatasetSelection {} not applicable to process {} anymore", dsSel.getId(),
                            processBusinessId);
                dsSel.setProcessDatasetDescription(null);
            });
            dsSelRepo.saveAll(dsSels);
        }
    }

}
