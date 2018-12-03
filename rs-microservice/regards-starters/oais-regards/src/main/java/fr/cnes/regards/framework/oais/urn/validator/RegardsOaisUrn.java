/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.urn.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * Annotation allowing to Validate {@link UniformResourceName} thanks to {@link RegardsOaisUrnValidator}
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = RegardsOaisUrnValidator.class)
@Documented
public @interface RegardsOaisUrn {

    /**
     * Class to validate
     */
    String CLASS_NAME = "RegardsOaisUrn.";

    /**
     * @return error message key
     */
    String message() default "{" + CLASS_NAME + "message}";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return custom payload
     */
    Class<? extends Payload>[] payload() default {};
}
