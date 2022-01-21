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
package fr.cnes.regards.modules.ingest.domain.request.update;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;

/**
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_update_task")
@DiscriminatorColumn(name = "dtype", length = 32)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractAIPUpdateTask {

    @Id
    @SequenceGenerator(name = "aipUpdateTaskSequence", initialValue = 1, sequenceName = "seq_aip_update_task")
    @GeneratedValue(generator = "aipUpdateTaskSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull(message = "AIP update state is required")
    @Enumerated(EnumType.STRING)
    private AIPUpdateState state;

    @NotNull(message = "Update task type is required")
    @Enumerated(EnumType.STRING)
    private AIPUpdateTaskType type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AIPUpdateState getState() {
        return state;
    }

    public void setState(AIPUpdateState state) {
        this.state = state;
    }

    public AIPUpdateTaskType getType() {
        return type;
    }

    public void setType(AIPUpdateTaskType type) {
        this.type = type;
    }

    public static List<AbstractAIPUpdateTask> build(AIPUpdateParametersDto updateTaskDto) {
        List<AbstractAIPUpdateTask> result = new ArrayList<>();
        if (!updateTaskDto.getAddCategories().isEmpty()) {
            result.add(AIPUpdateCategoryTask.build(AIPUpdateTaskType.ADD_CATEGORY, AIPUpdateState.READY,
                                                   updateTaskDto.getAddCategories()));
        }
        if (!updateTaskDto.getRemoveCategories().isEmpty()) {
            result.add(AIPUpdateCategoryTask.build(AIPUpdateTaskType.REMOVE_CATEGORY, AIPUpdateState.READY,
                                                   updateTaskDto.getRemoveCategories()));
        }
        if (!updateTaskDto.getAddTags().isEmpty()) {
            result.add(AIPUpdateTagTask.build(AIPUpdateTaskType.ADD_TAG, AIPUpdateState.READY,
                                              updateTaskDto.getAddTags()));
        }
        if (!updateTaskDto.getRemoveTags().isEmpty()) {
            result.add(AIPUpdateTagTask.build(AIPUpdateTaskType.REMOVE_TAG, AIPUpdateState.READY,
                                              updateTaskDto.getRemoveTags()));
        }
        if (!updateTaskDto.getRemoveStorages().isEmpty()) {
            result.add(AIPRemoveStorageTask.build(AIPUpdateTaskType.REMOVE_STORAGE, AIPUpdateState.READY,
                                                  updateTaskDto.getRemoveStorages()));
        }
        return result;
    }
}
