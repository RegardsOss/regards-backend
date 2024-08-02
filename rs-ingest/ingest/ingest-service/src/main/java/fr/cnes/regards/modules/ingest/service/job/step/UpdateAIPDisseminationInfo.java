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
package fr.cnes.regards.modules.ingest.service.job.step;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateDisseminationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.DisseminationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Update step to merge the {@link DisseminationInfo}s of an {@link AIPDto} with provided ones
 */
public class UpdateAIPDisseminationInfo implements IUpdateStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAIPDisseminationInfo.class);

    @Override
    public AIPEntityUpdateWrapper run(AIPEntityUpdateWrapper aipWrapper, AbstractAIPUpdateTask updateTask)
        throws ModuleException {
        AIPUpdateDisseminationTask updateDisseminationTask = (AIPUpdateDisseminationTask) updateTask;

        List<DisseminationInfo> providedDisseminationInfos = updateDisseminationTask.getDisseminationInfoUpdates();
        AIPEntity aip = aipWrapper.getAip();

        //Merge the aip's list of dissemination info with the provided list of dissemination info
        List<DisseminationInfo> mergedDisseminationInfos = mergeDisseminationInfoList(aip, providedDisseminationInfos);
        aip.setDisseminationInfos(mergedDisseminationInfos);

        //If there is at least one disseminationInfo with no ackDate, DisseminationStatus is PENDING
        if (mergedDisseminationInfos.stream().anyMatch(info -> !info.hasReceivedAck())) {
            aip.setDisseminationStatus(DisseminationStatus.PENDING);
        } else {
            aip.setDisseminationStatus(DisseminationStatus.DONE);
        }

        aipWrapper.markAsUpdated(true);
        return aipWrapper;
    }

    /**
     * Merge the provided {@link DisseminationInfo}s with the ones in the aips.
     * If a provided {@link DisseminationInfo} doesn't exists in aip, we add it
     * If a  {@link DisseminationInfo} in the aip is not among the provided ones, we keep it
     * If {@link DisseminationInfo} is in both lists, we update it
     */
    public List<DisseminationInfo> mergeDisseminationInfoList(AIPEntity aip,
                                                              List<DisseminationInfo> providedDisseminationInfos) {

        // Retrieve exising dissemination info from aip
        List<DisseminationInfo> existingDisseminationInfos = new ArrayList<>();
        if (aip.getDisseminationInfos() != null) {
            existingDisseminationInfos = aip.getDisseminationInfos();
        }

        // Retrieve new dissemination infos not present in the current aip
        List<DisseminationInfo> mergedDisseminationInfos = computeNewDisseminationInfos(existingDisseminationInfos,
                                                                                        providedDisseminationInfos);

        // Merge new dissemination infos with existing one in the current aip
        for (DisseminationInfo oldDisseminationInfo : existingDisseminationInfos) {
            Optional<DisseminationInfo> optionalDisseminationInfo = providedDisseminationInfos.stream()
                                                                                              .filter(
                                                                                                  newDisseminationInfo -> newDisseminationInfo.getLabel()
                                                                                                                                              .equals(
                                                                                                                                                  oldDisseminationInfo.getLabel()))
                                                                                              .findFirst();

            // If a dissemination info matches, merge the old one and the new one
            // Else keep the old info as it was
            optionalDisseminationInfo.ifPresentOrElse(disseminationInfo -> mergedDisseminationInfos.add(
                                                          mergeDisseminationInfo(oldDisseminationInfo, disseminationInfo)),
                                                      () -> mergedDisseminationInfos.add(oldDisseminationInfo));
        }

        return mergedDisseminationInfos;

    }

    private DisseminationInfo mergeDisseminationInfo(DisseminationInfo aipDisseminationInfo,
                                                     DisseminationInfo newDisseminationInfo) {
        // Merging two disseminationInfo is basically updating ackDate and date

        // Update dissemination date, only if :
        // - new dissemination date is not null
        // - current date is null OR new date is after current one
        if (newDisseminationInfo.getDate() != null && (aipDisseminationInfo.getDate() == null
                                                       || newDisseminationInfo.getDate()
                                                                              .isAfter(aipDisseminationInfo.getDate()))) {
            aipDisseminationInfo.setDate(newDisseminationInfo.getDate());
            if (newDisseminationInfo.getDate().isAfter(aipDisseminationInfo.getDate())) {
                // Reinit, ack date in case of new dissemination
                aipDisseminationInfo.setAckDate(null);
            }
        }

        // Update ack date, only if :
        // - new ack date is not null
        // - new ack date is after current dissemination date
        if (newDisseminationInfo.hasReceivedAck()) {
            // If ack is handled before initial dissemination date (async issue), then init the dissemination date to
            // the ack date.
            if (aipDisseminationInfo.getDate() == null) {
                aipDisseminationInfo.setDate(newDisseminationInfo.getAckDate());
            }
            if (newDisseminationInfo.getAckDate().isAfter(aipDisseminationInfo.getDate())
                || newDisseminationInfo.getAckDate().isEqual(aipDisseminationInfo.getDate())) {
                aipDisseminationInfo.setAckDate(newDisseminationInfo.getAckDate());
            }
        }
        return aipDisseminationInfo;
    }

    /**
     * Return the provided {@link DisseminationInfo}s that are not in the aip yet
     */
    private List<DisseminationInfo> computeNewDisseminationInfos(List<DisseminationInfo> aipDisseminationInfos,
                                                                 List<DisseminationInfo> providedDisseminationInfos) {

        List<String> labels = aipDisseminationInfos.stream().map(DisseminationInfo::getLabel).toList();

        return providedDisseminationInfos.stream()
                                         .filter(disseminationInfo -> !labels.contains(disseminationInfo.getLabel()))
                                         .map(this::initNewDisseminationInfo)
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toList());
    }

    private DisseminationInfo initNewDisseminationInfo(DisseminationInfo disseminationInfo) {
        if (disseminationInfo.getAckDate() == null && disseminationInfo.getDate() == null) {
            return null;
        }
        DisseminationInfo newDisseminationInfo = new DisseminationInfo(disseminationInfo.getLabel(),
                                                                       disseminationInfo.getDate(),
                                                                       disseminationInfo.getAckDate());

        // If dissemination info contains an ack date but no init date, set the init date to the ack date.
        // This case is to handle async issue between init and ack update requests (ack is received before init)
        if (newDisseminationInfo.getAckDate() != null && newDisseminationInfo.getDate() == null) {
            newDisseminationInfo.setDate(newDisseminationInfo.getAckDate());
        }
        return newDisseminationInfo;
    }
}
