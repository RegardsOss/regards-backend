/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * Handler to update AIPEntity state when a AIPEvent is received from archiva storage.
 * @author Sébastien Binda
 */
@Component
public class AIPEventHandler implements IHandler<AIPEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IPublisher publisher;

    @Override
    public void handle(TenantWrapper<AIPEvent> pWrapper) {
        AIPEvent event = pWrapper.getContent();
        switch (event.getAipState()) {
            case STORAGE_ERROR:
                handleStorageError(pWrapper.getTenant(), event.getIpId());
                break;
            case STORED:
                handleStorageSuccess(pWrapper.getTenant(), event.getIpId());
                break;
            case DELETED:
                handleDeleted(pWrapper.getTenant(), event.getIpId());
                break;
            case PENDING:
            case STORING_METADATA:
            case UPDATED:
            case VALID:
            default:
                break;

        }
    }

    private void handleStorageError(String tenant, String ipId) {
        // AIP Successfully stored
        runtimeTenantResolver.forceTenant(tenant);
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityState(AIPState.STORE_ERROR, aip.getId());
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.STORE_ERROR);
            sipRepository.updateSIPEntityState(SIPState.STORE_ERROR, sip.getId());
            publisher.publish(new SIPEvent(sip));

        }
        runtimeTenantResolver.clearTenant();
    }

    private void handleDeleted(String tenant, String ipId) {
        // AIP Deleted
        runtimeTenantResolver.forceTenant(tenant);
        // Check if deleted AIP exists in internal database
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Delete aip
            aipRepository.delete(oAip.get());
        }
        runtimeTenantResolver.clearTenant();
    }

    private void handleStorageSuccess(String tenant, String ipId) {
        // AIP Successfully stored
        runtimeTenantResolver.forceTenant(tenant);
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(AIPState.STORED);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
            // If all AIP are stored update SIP state to STORED
            Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
            if (sipAips.stream().allMatch(a -> AIPState.STORED.equals(a.getState()))) {
                SIPEntity sip = aip.getSip();
                sip.setState(SIPState.STORED);
                sipRepository.updateSIPEntityState(SIPState.STORED, sip.getId());
                publisher.publish(new SIPEvent(sip));
                // AIPs are no longer usefull here we can delete them
                aipRepository.delete(sipAips);
            }
        }
        runtimeTenantResolver.clearTenant();
    }

}
