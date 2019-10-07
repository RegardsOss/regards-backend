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
package fr.cnes.regards.modules.ingest.dto.request.update;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;

/**
 *
 * AIP update DTO task type adapter
 *
 * @author Léo Mieulet
 *
 */
public class AIPUpdateTaskDtoJsonAdapterFactory extends PolymorphicTypeAdapterFactory<AbstractAIPUpdateTaskDto> {

    protected AIPUpdateTaskDtoJsonAdapterFactory() {
        super(AbstractAIPUpdateTaskDto.class, "type");
        registerSubtype(AIPUpdateStorageTaskDto.class, AIPUpdateTaskType.ADD_STORAGE);
        registerSubtype(AIPUpdateStorageTaskDto.class, AIPUpdateTaskType.REMOVE_STORAGE);
        registerSubtype(AIPUpdateSimpleValueTaskDto.class, AIPUpdateTaskType.ADD_TAG);
        registerSubtype(AIPUpdateSimpleValueTaskDto.class, AIPUpdateTaskType.REMOVE_TAG);
        registerSubtype(AIPUpdateSimpleValueTaskDto.class, AIPUpdateTaskType.ADD_CATEGORY);
        registerSubtype(AIPUpdateSimpleValueTaskDto.class, AIPUpdateTaskType.REMOVE_CATEGORY);
    }
}
