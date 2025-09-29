/*
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
package fr.cnes.regards.modules.ingest.service.job.step;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.dto.InformationPackageMapDto;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateCategoryTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTagTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Update step to add/remove a Descriptive property from {@link InformationPackageMapDto} of an {@link AIPDto}
 *
 * @author Léo Mieulet
 */
public class UpdateAIPSimpleProperty implements IUpdateStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAIPSimpleProperty.class);

    @Override
    public AIPEntityUpdateWrapper run(AIPEntityUpdateWrapper aipWrapper, AbstractAIPUpdateTask updateTask)
        throws ModuleException {
        AIPUpdateTaskType taskType = updateTask.getType();
        return switch (taskType) {
            case ADD_CATEGORY, REMOVE_CATEGORY -> {
                AIPUpdateCategoryTask updateCategoryTask = (AIPUpdateCategoryTask) updateTask;
                yield handleCategory(aipWrapper, updateCategoryTask);
            }
            case ADD_TAG, REMOVE_TAG -> {
                AIPUpdateTagTask updateTagTask = (AIPUpdateTagTask) updateTask;
                yield handleTag(aipWrapper, updateTagTask);
            }
            default -> throw new ModuleException(String.format("Unexpected type of update request : %s", taskType));
        };
    }

    private AIPEntityUpdateWrapper handleTag(AIPEntityUpdateWrapper aipWrapper, AIPUpdateTagTask updateTask) {
        AIPEntity aip = aipWrapper.getAip();
        List<String> tags = updateTask.getTags();
        String[] tagsArray = tags.toArray(new String[tags.size()]);
        int tagSize = aip.getTags().size();
        if (AIPUpdateTaskType.ADD_TAG == updateTask.getType()) {
            aip.getAip().withContextTags(tagsArray);
            aip.getTags().addAll(tags);
        } else {
            aip.getAip().withoutContextTags(tagsArray);
            tags.forEach(aip.getTags()::remove);
        }
        // Update the wrapper pristine flag if the list changed
        if (tagSize != aip.getTags().size()) {
            aipWrapper.markAsUpdated(true);
        }
        return aipWrapper;
    }

    private AIPEntityUpdateWrapper handleCategory(AIPEntityUpdateWrapper aipWrapper, AIPUpdateCategoryTask updateTask)
        throws ModuleException {

        final AIPEntity aip = aipWrapper.getAip();
        String oldCategory = aip.getCategory();
        if (oldCategory != null && oldCategory.isBlank()) {
            oldCategory = null;
        }
        if (CollectionUtils.isEmpty(updateTask.getCategories())) {
            // There's nothing to do actually
            return aipWrapper;
        }

        if (AIPUpdateTaskType.ADD_CATEGORY == updateTask.getType()) {
            return handleAddCategory(aipWrapper, updateTask, oldCategory, aip);
        } else {
            return handleRemoveCategory(aipWrapper, updateTask, oldCategory, aip);
        }
    }

    private static AIPEntityUpdateWrapper handleAddCategory(AIPEntityUpdateWrapper aipWrapper,
                                                            AIPUpdateCategoryTask updateTask,
                                                            String oldCategory,
                                                            AIPEntity aip) throws ModuleException {
        if (updateTask.getCategories().size() > 1) {
            // Can't add several categories
            String msg = "Add category: the 'categories' parameter contains more than one element.";
            LOGGER.error(msg);
            throw new ModuleException(msg);
        }
        String newCategory = updateTask.getCategories().get(0);
        // Adding a category that is already there is a no-op
        if (!newCategory.equals(oldCategory)) {
            if (oldCategory != null) {
                // Can't add a category while there's already an existing one
                String msg = String.format("Add category: cannot add category %s, the feature already has a category. "
                                           + "Remove the existing category before adding a new one", newCategory);
                LOGGER.error(msg);
                throw new ModuleException(msg);
            }
            aip.setCategory(newCategory);
            aipWrapper.markAsUpdated(true);
        }
        return aipWrapper;
    }

    private static AIPEntityUpdateWrapper handleRemoveCategory(AIPEntityUpdateWrapper aipWrapper,
                                                               AIPUpdateCategoryTask updateTask,
                                                               String oldCategory,
                                                               AIPEntity aip) {
        // Removing a category that is not already present is a no-op
        if (oldCategory != null && updateTask.getCategories().contains(oldCategory)) {
            aip.setCategory(null);
            aipWrapper.markAsUpdated(true);
        }
        return aipWrapper;
    }

}
