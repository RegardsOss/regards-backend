/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;

/**
 * @author Kevin Marchois
 */
public class RegardsOaisUrnValidator implements ConstraintValidator<RegardsOaisUrn, OaisUniformResourceName> {

    @Override
    public void initialize(RegardsOaisUrn pConstraintAnnotation) {
        // nothing to initialize for now
    }

    @Override
    public boolean isValid(OaisUniformResourceName pValue, ConstraintValidatorContext pContext) {
        return (pValue == null) || !(pValue.getIdentifier().equals(OAISIdentifier.SIP.name())
                && ((pValue.getOrder() != null) || (pValue.getRevision() != null)));
    }

}