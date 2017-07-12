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
package fr.cnes.regards.framework.jpa.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;

/**
 * @author svissier
 *
 */
public class PastOrNowValidator implements ConstraintValidator<PastOrNow, OffsetDateTime> {

    @Override
    public void initialize(PastOrNow pArg0) {
        // Nothing to initialize for now
    }

    @Override
    public boolean isValid(OffsetDateTime date, ConstraintValidatorContext context) {
        OffsetDateTime now = OffsetDateTime.now();
        return (date == null) || !date.isAfter(now);
    }

}
