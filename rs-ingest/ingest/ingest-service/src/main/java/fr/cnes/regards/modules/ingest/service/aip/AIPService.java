/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedAipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * Service to handle aip related issues in ingest, including sending bulk request of AIP to store to archival storage
 * microservice.
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    @Autowired
    private IAIPRepository aipRepository;

    @Override
    public List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips) {
        List<AIPEntity> entities = new ArrayList<>();
        for (AIP aip : aips) {
            entities.add(aipRepository.save(AIPEntity.build(sip, AIPState.CREATED, aip)));
        }
        return entities;
    }

    @Override
    public void setAipToStored(UniformResourceName aipId, AIPState state) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(state);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
        }
    }

    @Override
    public Collection<RejectedAipDto> deleteAip(String sipId) {
        Set<RejectedAipDto> undeletableAips = Sets.newHashSet();

        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);
        // For each AIP,
        //      notify storage to delete related files
        //      mark the entity as TO_BE_DELETED
        for (AIPEntity aip : aipsRelatedToSip) {
            if (aip.getState() == AIPState.STORED) {
                // TODO
                //                FileDeletionRequestDTO toDelete = FileDeletionRequestDTO.build("cheksum", "storage", "owner", false);
                //                RequestInfo delete = storageClient.delete(toDelete);
                //                String groupId = delete.getGroupId();
                //                // TODO send event to delete on storage

                // TODO save inside a DB table this entity will be removed (keep removeIrrevocably too)
                // And listen for events from storage for this entity
                //                aip.setState(AIPState.TO_BE_DELETED);
                aipRepository.save(aip);
            } else {
                // We had this condition on those state here and not into #isDeletableWithAIPs because we just want to be silent.
                // Indeed, if we ask for deletion of an already deleted or being deleted SIP that just mean there is less work to do this time.
                String errorMsg = String.format("AIPEntity with state %s is not deletable", aip.getState());
                undeletableAips.add(RejectedAipDto.build(aip.getAipId(), errorMsg));
                // TODO gérer le cas ou la suppression n'est pas aussi simple
            }
        }
        return undeletableAips;
    }

    @Override
    public Optional<AIPEntity> searchAip(UniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        return aipRepository.save(entity);
    }

    @Override
    public void askForAipsDeletion() {
        // TODO Auto-generated method stub
        // TODO refactor with files only
    }
}
