/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.store;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;

/**
 * Handler to listen all entity events to detect entity indexation.
 * When an entity is indexed, update the SIP associated state to indexed.
 * @author Sébastien Binda
 */
@Component
public class BroadcastEntityEventHandler
        implements IHandler<BroadcastEntityEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastEntityEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IAIPRepository aipRepository;

    /**
     * {@link ISubscriber} instance
     */
    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(BroadcastEntityEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<BroadcastEntityEvent> wrapper) {
        BroadcastEntityEvent event = wrapper.getContent();
        LOGGER.info("BroadcastEntityEvent received");
        if ((event != null) && (event.getEventType() != null)) {
            LOGGER.info("BroadcastEntityEvent received type={}", event.getEventType());
            switch (event.getEventType()) {
                case INDEXED:
                    handleEntitiesIndexed(event.getAipIds(), wrapper.getTenant());
                    break;
                case INDEX_ERROR:
                    handleEntitiesNotIndexed(event.getAipIds(), wrapper.getTenant());
                case DELETE:
                case CREATE:
                case UPDATE:
                default:
                    // Nothing to do
                    break;
            }
        }
    }

    /**
     * Handle the case of entities indexed
     * @param ipIds of all new indexed entities
     */
    private void handleEntitiesIndexed(UniformResourceName[] ipIds, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        // Check if AIPs matchs ipIds
        for (UniformResourceName ipId : ipIds) {
            Optional<AIPEntity> oAip = aipService.searchAip(ipId);
            if (oAip.isPresent()) {
                AIPEntity aip = oAip.get();
                aipService.setAipToIndexed(aip);
                LOGGER.info("AIP \"{}\" is now indexed.", ipId.toString());
                // If all AIPs of current SIP are indexed then update SIP state to INDEXED
                Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
                if (sipAips.stream().allMatch(aipEntity -> aipEntity.getState() == SipAIPState.INDEXED)) {
                    SIPEntity sip = aip.getSip();
                    sip.setState(SIPState.INDEXED);
                    sipService.saveSIPEntity(sip);
                    LOGGER.info("SIP \"{}\" is now indexed.", sip.getSipId());
                    // AIPs are no longer useful here we can delete them
                    aipRepository.delete(sipAips);
                }
            }
        }
        runtimeTenantResolver.clearTenant();
    }

    /**
     * Handle the case of entities not indexed (due to error of course)
     * @param ipIds of all entities that cannot be indexed
     */
    private void handleEntitiesNotIndexed(UniformResourceName[] ipIds, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        // Check if AIPs matchs ipIds
        for (UniformResourceName ipId : ipIds) {
            Optional<AIPEntity> oAip = aipService.searchAip(ipId);
            if (oAip.isPresent()) {
                AIPEntity aip = oAip.get();
                aipService.setAipToIndexed(aip);
                LOGGER.info("AIP \"{}\" is now indexed.", ipId.toString());
                // If one AIP of current SIP is at INDEX_ERROR than update SIP state to INDEX_ERROR
                Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
                if (sipAips.stream().anyMatch(aipEntity -> aipEntity.getState() == SipAIPState.INDEX_ERROR)) {
                    SIPEntity sip = aip.getSip();
                    sip.setState(SIPState.INDEX_ERROR);
                    sipService.saveSIPEntity(sip);
                    LOGGER.info("SIP \"{}\" cannot be indexed.", sip.getSipId());
                }
            }
        }
        runtimeTenantResolver.clearTenant();
    }

}
