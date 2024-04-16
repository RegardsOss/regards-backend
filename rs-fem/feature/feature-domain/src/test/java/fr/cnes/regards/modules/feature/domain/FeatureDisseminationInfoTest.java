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
package fr.cnes.regards.modules.feature.domain;

import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;

/**
 * @author Sébastien Binda
 **/
public class FeatureDisseminationInfoTest {

    @Test
    public void test_dissemination_info_update_nominal_without_ack() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");

        // When
        featureDisseminationInfo.updateRequestDate(putDate, false);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isAcknowledged());
        Assert.assertEquals(putDate, featureDisseminationInfo.getRequestDate());
        Assert.assertEquals(putDate, featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_update_renotification_with_ack() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        OffsetDateTime ackDate = OffsetDateTime.now().minusSeconds(5);
        OffsetDateTime newPutDate = OffsetDateTime.now().minusSeconds(2);
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");
        featureDisseminationInfo.setRequestDate(putDate);
        featureDisseminationInfo.updateAckDate(ackDate);

        // When
        featureDisseminationInfo.updateRequestDate(newPutDate, true);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isWaitingAck());
        Assert.assertEquals(newPutDate, featureDisseminationInfo.getRequestDate());
        Assert.assertNull(featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_update_renotification_without_ack() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        OffsetDateTime newPutDate = OffsetDateTime.now().minusSeconds(2);
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");
        featureDisseminationInfo.setRequestDate(putDate);
        featureDisseminationInfo.setAckDate(putDate);

        // When
        featureDisseminationInfo.updateRequestDate(newPutDate, false);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isAcknowledged());
        Assert.assertEquals(newPutDate, featureDisseminationInfo.getRequestDate());
        Assert.assertEquals(newPutDate, featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_update_put_after_ack() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        OffsetDateTime ackDate = OffsetDateTime.now();
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");
        featureDisseminationInfo.setRequestDate(ackDate);
        featureDisseminationInfo.setAckDate(ackDate);

        // When
        featureDisseminationInfo.updateRequestDate(putDate, true);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isAcknowledged());
        Assert.assertEquals(ackDate, featureDisseminationInfo.getRequestDate());
        Assert.assertEquals(ackDate, featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_update_nominal_with_ack() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");

        // When
        featureDisseminationInfo.updateRequestDate(putDate, true);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isWaitingAck());
        Assert.assertEquals(featureDisseminationInfo.getRequestDate(), putDate);
        Assert.assertNull(featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_ack_nominal() {
        // Given
        OffsetDateTime putDate = OffsetDateTime.now().minusSeconds(10);
        OffsetDateTime ackDate = OffsetDateTime.now();
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");
        featureDisseminationInfo.setRequestDate(putDate);

        // When
        featureDisseminationInfo.updateAckDate(ackDate);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isAcknowledged());
        Assert.assertEquals(featureDisseminationInfo.getRequestDate(), putDate);
        Assert.assertEquals(ackDate, featureDisseminationInfo.getAckDate());

    }

    @Test
    public void test_dissemination_info_ack_before_put() {
        // Given
        OffsetDateTime ackDate = OffsetDateTime.now();
        FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo();
        featureDisseminationInfo.setLabel("test");

        // When
        featureDisseminationInfo.updateAckDate(ackDate);

        // Then
        Assert.assertTrue(featureDisseminationInfo.isAcknowledged());
        Assert.assertEquals(ackDate, featureDisseminationInfo.getRequestDate());
        Assert.assertEquals(ackDate, featureDisseminationInfo.getAckDate());

    }

}
