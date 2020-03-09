/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.sip.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * Check that reference (i.e. ref attribute not null) or value (i.e. properties attribute not null) is set.
 *
 * @author Marc Sordi
 *
 */
public class CheckSIPValidator implements ConstraintValidator<CheckSIP, SIP> {

    @Override
    public void initialize(CheckSIP constraintAnnotation) {
        // Nothing to initialize
    }

    @Override
    public boolean isValid(SIP sip, ConstraintValidatorContext context) {
        if ((sip.getRef() == null) && (sip.getProperties() == null)) {
            return false;
        }
        return ((sip.getRef() == null) || (sip.getProperties() == null));
    }
}
