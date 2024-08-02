package fr.cnes.regards.modules.crawler.service.exception;/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.Serial;

/**
 * Should be thrown only when first find of {@link fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin} encounters an exception.
 */
public class FirstFindException extends Exception {

    @Serial
    private static final long serialVersionUID = 8422354822435901366L;

    public FirstFindException(Throwable e) {
        super(e);
    }

}
