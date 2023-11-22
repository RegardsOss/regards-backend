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
package fr.cnes.regards.modules.ingest.dto.request.update;

import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Object containing some AIP criteria and a list of tags, categories and location to add or remove from these AIPs
 *
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
     * Dissemination info to update on each AIPs
     */
    private List<DisseminationInfo> updateDisseminationInfo;

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

    public List<DisseminationInfo> getUpdateDisseminationInfo() {
        return updateDisseminationInfo;
    }

    public void setUpdateDisseminationInfo(List<DisseminationInfo> updateDisseminationInfo) {
        this.updateDisseminationInfo = updateDisseminationInfo;
    }

    public static AIPUpdateParametersDto build(SearchAIPsParameters aipsParameters,
                                               List<String> addTags,
                                               List<String> removeTags,
                                               List<String> addCategories,
                                               List<String> removeCategories,
                                               List<String> removeStorages,
                                               List<DisseminationInfo> disseminationInfoUpdates) {
        AIPUpdateParametersDto params = new AIPUpdateParametersDto();
        params.setCriteria(aipsParameters);
        params.setAddTags(addTags);
        params.setRemoveTags(removeTags);
        params.setAddCategories(addCategories);
        params.setRemoveCategories(removeCategories);
        params.setRemoveStorages(removeStorages);
        params.setUpdateDisseminationInfo(disseminationInfoUpdates);
        return params;
    }

    public static AIPUpdateParametersDto build(SearchAIPsParameters aipsParameters) {
        AIPUpdateParametersDto params = new AIPUpdateParametersDto();
        params.setCriteria(aipsParameters);
        params.setAddTags(new ArrayList<>());
        params.setRemoveTags(new ArrayList<>());
        params.setAddCategories(new ArrayList<>());
        params.setRemoveCategories(new ArrayList<>());
        params.setRemoveStorages(new ArrayList<>());
        params.setUpdateDisseminationInfo(new ArrayList<>());
        return params;
    }

    public static AIPUpdateParametersDto build() {
        return build(null);
    }

    public AIPUpdateParametersDto withAddTags(List<String> addTags) {
        this.addTags.addAll(addTags);
        return this;
    }

    public AIPUpdateParametersDto withRemoveTags(List<String> removeTags) {
        this.removeTags.addAll(removeTags);
        return this;
    }

    public AIPUpdateParametersDto withAddCategories(List<String> addCategories) {
        this.addCategories.addAll(addCategories);
        return this;
    }

    public AIPUpdateParametersDto withRemoveCategories(List<String> removeCategories) {
        this.removeCategories.addAll(removeCategories);
        return this;
    }

    public AIPUpdateParametersDto withRemoveStorages(List<String> removeStorages) {
        this.removeStorages.addAll(removeStorages);
        return this;
    }
}
