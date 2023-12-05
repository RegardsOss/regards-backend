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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.service.job.step;

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationStatus;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateDisseminationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

        if (aip.getDisseminationInfos() == null) {
            return providedDisseminationInfos;
        } else {
            //We add the dissemination infos not present in the aip
            List<DisseminationInfo> mergedDisseminationInfos = computeNewDisseminationInfos(aip.getDisseminationInfos(),
                                                                                            providedDisseminationInfos);

            //If we find two disseminationInfo in the aip and in the provided with the same label, we merge it
            for (DisseminationInfo oldDisseminationInfo : aip.getDisseminationInfos()) {

                Optional<DisseminationInfo> optionalDisseminationInfo = providedDisseminationInfos.stream()
                                                                                                  .filter(
                                                                                                      newDisseminationInfo -> newDisseminationInfo.getLabel()
                                                                                                                                                  .equals(
                                                                                                                                                      oldDisseminationInfo.getLabel()))
                                                                                                  .findFirst();

                //If a dissemination info matches, we merge the old info and the new info
                // else we keep the old info as it was
                optionalDisseminationInfo.ifPresentOrElse(disseminationInfo -> mergedDisseminationInfos.add(
                                                              mergeDisseminationInfo(oldDisseminationInfo, disseminationInfo)),
                                                          () -> mergedDisseminationInfos.add(oldDisseminationInfo));
            }

            return mergedDisseminationInfos;
        }
    }

    private DisseminationInfo mergeDisseminationInfo(DisseminationInfo aipDisseminationInfo,
                                                     DisseminationInfo newDisseminationInfo) {
        //Merging two disseminationInfo is basically updating ackDate and date (ackDate can not be overwritten)
        if (!aipDisseminationInfo.hasReceivedAck() && newDisseminationInfo.hasReceivedAck()) {
            aipDisseminationInfo.setAckDate(newDisseminationInfo.getAckDate());
        }
        if (newDisseminationInfo.hasInitialDate()) {
            aipDisseminationInfo.setDate(newDisseminationInfo.getDate());
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
                                         .collect(Collectors.toList());
    }
}
