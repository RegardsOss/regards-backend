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
package fr.cnes.regards.modules.ingest.domain.entity.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.web.bind.annotation.ControllerAdvice;

import fr.cnes.regards.modules.ingest.domain.entity.AipStateManager;
import fr.cnes.regards.modules.storage.domain.IAipState;

/**
 * Implement the type conversion logic for a String to a {@link IAipState}.<br>
 * This is automatically used by Spring if need be.
 *
 * @author Marc Sordi
 */
@ControllerAdvice
public class StringToAipState implements Converter<String, IAipState> {

    @Override
    public IAipState convert(String pSource) {
        return AipStateManager.fromName(pSource);
    }
}
