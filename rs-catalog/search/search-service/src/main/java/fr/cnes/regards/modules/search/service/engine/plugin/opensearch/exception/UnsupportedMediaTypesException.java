/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception;

import java.util.List;

import org.springframework.http.MediaType;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * UnsupportedMediaTypesException
 * @author Sébastien Binda
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypesException extends ModuleException {

    private final List<MediaType> mediaTypes;

    public UnsupportedMediaTypesException(List<MediaType> mediaTypes) {
        super();
        this.mediaTypes = mediaTypes;
    }

    @Override
    public String getMessage() {
        String mediaTypesStr = mediaTypes.stream().reduce("", (r, m) -> String.format("%s, %s", r, m.getType()),
                                                          (s1, s2) -> String.format("%s, %s", s1, s2));
        return String.format("Unsupported media type %s", mediaTypesStr);
    }

}
