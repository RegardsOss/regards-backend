/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.validation.test.domain;

import java.time.LocalDateTime;

import fr.cnes.regards.modules.core.validation.PastOrNow;

/**
 * @author svissier
 *
 */
public class PastOrNowDate {

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
