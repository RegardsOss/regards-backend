/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.domain.annotations;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.domain.validation.PluginServicesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Assure that annotated {@link PluginConfiguration} is a plugin configuration of an {@link IService} plugin
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Constraint(validatedBy = PluginServicesValidator.class)
@Documented
public @interface PluginServices {

    /**
     * Class to validate
     */
    static final String CLASS_NAME = "fr.cnes.regards.modules.catalog.services.domain.validation.PluginServices.";

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
