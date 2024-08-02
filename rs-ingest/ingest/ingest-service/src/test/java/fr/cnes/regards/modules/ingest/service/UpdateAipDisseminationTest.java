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
package fr.cnes.regards.modules.ingest.service;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPDisseminationInfo;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * @author Thomas GUILLOU
 **/
public class UpdateAipDisseminationTest {

    private static String LABEL = "diss1";

    private static String LABEL2 = "diss2";

    private static String LABEL3 = "diss3";

    private static OffsetDateTime REFERENCE_DATE = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    private static OffsetDateTime REFERENCE_DATE2 = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    private static OffsetDateTime REFERENCE_DATE3 = OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    private static OffsetDateTime REFERENCE_DATE4 = OffsetDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    @Test
    public void test_multiple_update_aip_dissemination_date_and_ack_date() {
        UpdateAIPDisseminationInfo updateAIPDisseminationInfo = new UpdateAIPDisseminationInfo();

        // Given init AIP and dissemination info updates
        // Init AIP wih two pending dissemination :
        // - Dissemination 1 : label: LABEL, date: REFERENCE_DATE, ack_date: null
        // - Dissemination 2 : label: LABEL2, date: REFERENCE_DATE2, ack_date: null
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setDisseminationInfos(List.of(new DisseminationInfo(LABEL, REFERENCE_DATE, null),
                                                new DisseminationInfo(LABEL2, REFERENCE_DATE2, null)));

        // Init update dissemination info with ack for LABEL2 and new info for LABEL3
        // - Dissemination 2 : label: LABEL2, date: null, ack_date: REFERENCE_DATE3
        // - Dissemination 3 : label: LABEL3, date: REFERENCE_DATE4, ack_date: null
        List<DisseminationInfo> newListOfDissemination = List.of(new DisseminationInfo(LABEL2, null, REFERENCE_DATE3),
                                                                 new DisseminationInfo(LABEL3, REFERENCE_DATE4, null));
        List<DisseminationInfo> mergedDisseminationInfos = updateAIPDisseminationInfo.mergeDisseminationInfoList(
            aipEntity,
            newListOfDissemination);

        // When, merge AIP and dissemination info updates
        Assertions.assertEquals(3, mergedDisseminationInfos.size());

        // Then result is :
        // - Dissemination 1 : label: LABEL, date: REFERENCE_DATE, ack_date: null
        // - Dissemination 2 : label: LABEL2, date: REFERENCE_DATE2, ack_date: REFERENCE_DATE3
        // - Dissemination 3 : label: LABEL3, date: REFERENCE_DATE4, ack_date: null
        Assertions.assertTrue(getDisseminationInfo(mergedDisseminationInfos, LABEL).isPresent());
        Assertions.assertTrue(getDisseminationInfo(mergedDisseminationInfos, LABEL2).isPresent());
        Assertions.assertTrue(getDisseminationInfo(mergedDisseminationInfos, LABEL3).isPresent());
        Assertions.assertEquals(REFERENCE_DATE, getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getDate());
        Assertions.assertNull(getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getAckDate());
        Assertions.assertEquals(REFERENCE_DATE2,
                                getDisseminationInfo(mergedDisseminationInfos, LABEL2).get().getDate());
        Assertions.assertEquals(REFERENCE_DATE3,
                                getDisseminationInfo(mergedDisseminationInfos, LABEL2).get().getAckDate());
        Assertions.assertEquals(REFERENCE_DATE4,
                                getDisseminationInfo(mergedDisseminationInfos, LABEL3).get().getDate());
        Assertions.assertNull(getDisseminationInfo(mergedDisseminationInfos, LABEL3).get().getAckDate());
    }

    @Test
    public void test_new_dissemination_before_ack_received() {
        // Given init AIP and dissemination info updates
        UpdateAIPDisseminationInfo updateAIPDisseminationInfo = new UpdateAIPDisseminationInfo();

        // Init AIP wih one pending dissemination :
        // - Dissemination 1 : label: LABEL, date: REFERENCE_DATE, ack_date: null
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setDisseminationInfos(List.of(new DisseminationInfo(LABEL, REFERENCE_DATE, null)));

        // Init update dissemination info with ack for LABEL
        // - Dissemination 1 : label: LABEL, date: null, ack_date: REFERENCE_DATE2
        List<DisseminationInfo> newListOfDissemination = List.of(new DisseminationInfo(LABEL, REFERENCE_DATE2, null));

        // When, merge AIP and dissemination info updates
        List<DisseminationInfo> mergedDisseminationInfos = updateAIPDisseminationInfo.mergeDisseminationInfoList(
            aipEntity,
            newListOfDissemination);

        // Then dissemination date is updated
        // - Dissemination 1 : label: LABEL, date: REFERENCE_DATE2, ack_date: null
        Assertions.assertEquals(1, mergedDisseminationInfos.size());
        Assertions.assertTrue(getDisseminationInfo(mergedDisseminationInfos, LABEL).isPresent());
        Assertions.assertEquals(REFERENCE_DATE2, getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getDate());
        Assertions.assertNull(getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getAckDate());
    }

    @Test
    public void test_dissemination_ack_received_before_init() {
        // Given init AIP and dissemination info updates
        UpdateAIPDisseminationInfo updateAIPDisseminationInfo = new UpdateAIPDisseminationInfo();

        // Init AIP wih no pending dissemination :
        AIPEntity aipEntity = new AIPEntity();

        // Init update dissemination info with ack for LABEL
        // - Dissemination 2 : label: LABEL, date: null, ack_date: REFERENCE_DATE
        List<DisseminationInfo> newListOfDissemination = List.of(new DisseminationInfo(LABEL, null, REFERENCE_DATE));

        // When, merge AIP and dissemination info updates
        List<DisseminationInfo> mergedDisseminationInfos = updateAIPDisseminationInfo.mergeDisseminationInfoList(
            aipEntity,
            newListOfDissemination);

        // Then dissemination date is updated
        // - Dissemination 1 : label: LABEL, date: REFERENCE_DATE, ack_date: REFERENCE_DATE
        Assertions.assertEquals(1, mergedDisseminationInfos.size());
        Assertions.assertTrue(getDisseminationInfo(mergedDisseminationInfos, LABEL).isPresent());
        Assertions.assertEquals(REFERENCE_DATE, getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getDate());
        Assertions.assertEquals(REFERENCE_DATE,
                                getDisseminationInfo(mergedDisseminationInfos, LABEL).get().getAckDate());
    }

    private Optional<DisseminationInfo> getDisseminationInfo(List<DisseminationInfo> mergedDisseminationInfos,
                                                             String label) {
        return mergedDisseminationInfos.stream()
                                       .filter(disseminationInfo -> disseminationInfo.getLabel().equals(label))
                                       .findFirst();
    }
}
