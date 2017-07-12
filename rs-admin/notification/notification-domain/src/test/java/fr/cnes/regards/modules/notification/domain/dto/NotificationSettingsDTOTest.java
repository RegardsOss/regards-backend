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
package fr.cnes.regards.modules.notification.domain.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;

/**
 * Validate getter/setters on {@link NotificationSettingsDTO}.
 *
 * @author xbrochar
 */
public class NotificationSettingsDTOTest {

    /**
     * Days
     */
    private static final Integer DAYS = 1;

    /**
     * Hours
     */
    private static final Integer HOURS = 2;

    /**
     * Hours
     */
    private static final NotificationFrequency FREQUENCY = NotificationFrequency.MONTHLY;

    /**
     * Tested pojo
     */
    private NotificationSettingsDTO dto;

    /**
     * Setup a vaid notification before each test in order to unvalidate on by one
     */
    @Before
    public void setupNotification() {
        dto = new NotificationSettingsDTO();
        dto.setDays(DAYS);
        dto.setHours(HOURS);
        dto.setFrequency(FREQUENCY);
    }

    /**
     * Check the POJO getters/setters.
     */
    @Test
    @Requirement("?")
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertTrue(dto.getDays().equals(DAYS));
        Assert.assertTrue(dto.getHours().equals(HOURS));
        Assert.assertTrue(dto.getFrequency().equals(FREQUENCY));
    }

}
