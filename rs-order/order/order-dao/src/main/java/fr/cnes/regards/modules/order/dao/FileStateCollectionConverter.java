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
package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.modules.order.domain.FileState;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author oroussel
 */
@Converter
public class FileStateCollectionConverter implements AttributeConverter<Collection<FileState>, Collection<String>> {

    @Override
    public Collection<String> convertToDatabaseColumn(Collection<FileState> attribute) {
        return attribute.stream().map(e -> e.toString()).collect(Collectors.toList());
    }

    @Override
    public Collection<FileState> convertToEntityAttribute(Collection<String> dbData) {
        return dbData.stream().map(d -> FileState.valueOf(d)).collect(Collectors.toList());
    }
}
