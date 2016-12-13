/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters.sample7;

import java.time.LocalDateTime;

/**
 * @author Marc Sordi
 *
 */
public class DateProperty extends AbstractProperty<LocalDateTime> {

    private LocalDateTime value;

    @Override
    public LocalDateTime getValue() {
        return value;
    }

    public void setValue(LocalDateTime pValue) {
        value = pValue;
    }

}
