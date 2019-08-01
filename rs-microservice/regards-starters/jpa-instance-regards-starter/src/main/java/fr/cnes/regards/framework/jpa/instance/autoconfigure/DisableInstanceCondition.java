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
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Class DisableInstanceCondition
 *
 * Spring condition to disable instanceEntity filter for JPA entities and reposiroties
 * @author Sébastien Binda
 */
public class DisableInstanceCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext pArg0, final AnnotatedTypeMetadata pArg1) {
        final String result = pArg0.getEnvironment()
                .getProperty("regards.jpa.instance.disableInstanceEntityAnnotation");
        return Boolean.parseBoolean(result);
    }

}
