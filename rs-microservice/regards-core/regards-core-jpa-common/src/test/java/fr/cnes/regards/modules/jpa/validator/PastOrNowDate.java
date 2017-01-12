/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jpa.validator;

import java.time.LocalDateTime;

import fr.cnes.regards.framework.jpa.validator.PastOrNow;

/**
 * @author svissier
 *
 */
public class PastOrNowDate {

    /**
     * Test date
     */
    @PastOrNow
    private LocalDateTime dateTime;

    public PastOrNowDate(LocalDateTime pDateTime) {
        dateTime = pDateTime;

    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime pDateTime) {
        dateTime = pDateTime;
    }

}
