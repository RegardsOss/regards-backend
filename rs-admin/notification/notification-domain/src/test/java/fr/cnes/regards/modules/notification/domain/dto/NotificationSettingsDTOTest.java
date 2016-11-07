/*
 * LICENSE_PLACEHOLDER
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
