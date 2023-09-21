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

    private static OffsetDateTime REFERENCE_DATE5 = OffsetDateTime.of(2023, 2, 1, 1, 1, 1, 1, ZoneOffset.UTC);

    @Test
    public void test() {
        UpdateAIPDisseminationInfo updateAIPDisseminationInfo = new UpdateAIPDisseminationInfo();
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setDisseminationInfos(List.of(new DisseminationInfo(LABEL, REFERENCE_DATE, null),
                                                new DisseminationInfo(LABEL2, REFERENCE_DATE2, null)));
        List<DisseminationInfo> newListOfDissemination = List.of(new DisseminationInfo(LABEL2, null, REFERENCE_DATE3),
                                                                 new DisseminationInfo(LABEL3, REFERENCE_DATE4, null));
        List<DisseminationInfo> mergedDisseminationInfos = updateAIPDisseminationInfo.mergeDisseminationInfoList(
            aipEntity,
            newListOfDissemination);
        Assertions.assertEquals(3, mergedDisseminationInfos.size());

        Optional<DisseminationInfo> optionalDisseminationInfo = mergedDisseminationInfos.stream()
                                                                                        .filter(disseminationInfo -> disseminationInfo.getLabel()
                                                                                                                                      .equals(
                                                                                                                                          LABEL))
                                                                                        .findFirst();
        Optional<DisseminationInfo> optionalDisseminationInfo2 = mergedDisseminationInfos.stream()
                                                                                         .filter(disseminationInfo -> disseminationInfo.getLabel()
                                                                                                                                       .equals(
                                                                                                                                           LABEL2))
                                                                                         .findFirst();
        Optional<DisseminationInfo> optionalDisseminationInfo3 = mergedDisseminationInfos.stream()
                                                                                         .filter(disseminationInfo -> disseminationInfo.getLabel()
                                                                                                                                       .equals(
                                                                                                                                           LABEL3))
                                                                                         .findFirst();
        Assertions.assertTrue(optionalDisseminationInfo.isPresent());
        Assertions.assertTrue(optionalDisseminationInfo2.isPresent());
        Assertions.assertTrue(optionalDisseminationInfo3.isPresent());
        Assertions.assertTrue(optionalDisseminationInfo.get().getDate().equals(REFERENCE_DATE));
        Assertions.assertNull(optionalDisseminationInfo.get().getAckDate());
        Assertions.assertTrue(optionalDisseminationInfo2.get().getDate().equals(REFERENCE_DATE2));
        Assertions.assertTrue(optionalDisseminationInfo2.get().getAckDate().equals(REFERENCE_DATE3));
        Assertions.assertTrue(optionalDisseminationInfo3.get().getDate().equals(REFERENCE_DATE4));
        Assertions.assertNull(optionalDisseminationInfo3.get().getAckDate());

        aipEntity.setDisseminationInfos(mergedDisseminationInfos);
        newListOfDissemination = List.of(new DisseminationInfo(LABEL3, REFERENCE_DATE5, null),
                                         new DisseminationInfo(LABEL2, null, REFERENCE_DATE5));
        mergedDisseminationInfos = updateAIPDisseminationInfo.mergeDisseminationInfoList(aipEntity,
                                                                                         newListOfDissemination);

        Assertions.assertEquals(3, mergedDisseminationInfos.size());
        //ack date is not overwritten but date is
        Assertions.assertTrue(optionalDisseminationInfo2.get().getAckDate().equals(REFERENCE_DATE3));
        Assertions.assertTrue(optionalDisseminationInfo3.get().getDate().equals(REFERENCE_DATE5));

    }
}
