/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jpa.validator;

import java.time.OffsetDateTime;

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
    private OffsetDateTime dateTime;

    public PastOrNowDate(OffsetDateTime pDateTime) {
        dateTime = pDateTime;

    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(OffsetDateTime pDateTime) {
        dateTime = pDateTime;
    }

}
