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
package fr.cnes.regards.modules.ingest.service.job.step;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateCategoryTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTagTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * Update step to add/remove a Descriptive property from {@link InformationPackageMap} of an {@link AIP}
 *
 * @author Léo Mieulet
 */
public class UpdateAIPSimpleProperty implements IUpdateStep {

    @Override
    public AIPEntityUpdateWrapper run(AIPEntityUpdateWrapper aipWrapper, AbstractAIPUpdateTask updateTask)
            throws ModuleException {
        AIPUpdateTaskType taskType = updateTask.getType();
        switch (taskType) {
            case ADD_CATEGORY:
            case REMOVE_CATEGORY:
                AIPUpdateCategoryTask updateCategoryTask = (AIPUpdateCategoryTask) updateTask;
                return handleCategory(aipWrapper, updateCategoryTask);
            case ADD_TAG:
            case REMOVE_TAG:
                AIPUpdateTagTask updateTagTask = (AIPUpdateTagTask) updateTask;
                return handleTag(aipWrapper, updateTagTask);
            default:
                throw new ModuleException(String.format("Unexpected type of update request : %s", taskType));
        }
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
            aip.getTags().removeAll(tags);
        }
        // Update the wrapper pristine flag if the list changed
        if (tagSize != aip.getTags().size()) {
            aipWrapper.markAsUpdated(true);
        }
        return aipWrapper;
    }

    private AIPEntityUpdateWrapper handleCategory(AIPEntityUpdateWrapper aipWrapper, AIPUpdateCategoryTask updateTask) {
        AIPEntity aip = aipWrapper.getAip();
        List<String> categories = updateTask.getCategories();
        int categorySize = aip.getCategories().size();
        if (AIPUpdateTaskType.ADD_CATEGORY == updateTask.getType()) {
            aip.getCategories().addAll(categories);
        } else {
            aip.getCategories().removeAll(categories);
        }
        // Update the wrapper pristine flag if the list changed
        if (categorySize != aip.getCategories().size()) {
            aipWrapper.markAsUpdated(true);
        }
        return aipWrapper;
    }
}
