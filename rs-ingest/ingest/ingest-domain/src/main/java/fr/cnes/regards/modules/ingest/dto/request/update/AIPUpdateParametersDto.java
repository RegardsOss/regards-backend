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

import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import java.util.List;
import javax.validation.Valid;

/**
 * Object containing some AIP criteria and a list of tags, categories and location to add or remove from these AIPs
 * @author Léo Mieulet
 */
public class AIPUpdateParametersDto {

    /**
     * Criteria to filter AIPEntity
     */
    @Valid
    private SearchAIPsParameters criteria;

    /**
     * Tags to add on each AIPs
     */
    private List<String> addTags;

    /**
     * Tags to remove on each AIPs
     */
    private List<String> removeTags;

    /**
     * Categories to add on each AIPs
     */
    private List<String> addCategories;

    /**
     * Categories to remove on each AIPs
     */
    private List<String> removeCategories;

    /**
     * Storage business id to remove on each AIP dataobject
     */
    private List<String> removeStorages;

    public SearchAIPsParameters getCriteria() {
        return criteria;
    }

    public void setCriteria(SearchAIPsParameters criteria) {
        this.criteria = criteria;
    }

    public List<String> getAddTags() {
        return addTags;
    }

    public void setAddTags(List<String> addTags) {
        this.addTags = addTags;
    }

    public List<String> getRemoveTags() {
        return removeTags;
    }

    public void setRemoveTags(List<String> removeTags) {
        this.removeTags = removeTags;
    }

    public List<String> getAddCategories() {
        return addCategories;
    }

    public void setAddCategories(List<String> addCategories) {
        this.addCategories = addCategories;
    }

    public List<String> getRemoveCategories() {
        return removeCategories;
    }

    public void setRemoveCategories(List<String> removeCategories) {
        this.removeCategories = removeCategories;
    }

    public List<String> getRemoveStorages() {
        return removeStorages;
    }

    public void setRemoveStorages(List<String> removeStorages) {
        this.removeStorages = removeStorages;
    }

    public static AIPUpdateParametersDto build(SearchAIPsParameters aipsParameters, List<String> addTags,
            List<String> removeTags, List<String> addCategories, List<String> removeCategories, List<String> removeStorages) {
        AIPUpdateParametersDto params = new AIPUpdateParametersDto();
        params.setCriteria(aipsParameters);
        params.setAddTags(addTags);
        params.setRemoveTags(removeTags);
        params.setAddCategories(addCategories);
        params.setRemoveCategories(removeCategories);
        params.setRemoveStorages(removeStorages);
        return params;
    }
}
