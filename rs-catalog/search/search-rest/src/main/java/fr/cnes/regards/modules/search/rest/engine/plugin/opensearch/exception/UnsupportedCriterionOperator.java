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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * UnsupportedMediaTypesException
 * @author Sébastien Binda
 */
@SuppressWarnings("serial")
public class UnsupportedCriterionOperator extends ModuleException {

    public UnsupportedCriterionOperator() {
        super();
    }

    public UnsupportedCriterionOperator(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnsupportedCriterionOperator(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedCriterionOperator(String message) {
        super(message);
    }

    public UnsupportedCriterionOperator(Throwable cause) {
        super(cause);
    }

}
