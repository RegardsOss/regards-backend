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

/**
 * Parameters to update a list of AIPEntity matching provided criteria
 * The task to update is generic
 * @author Léo Mieulet
 */
public class AIPUpdateParameters {

    /**
     * Criteria to filter AIPEntity
     */
    private SearchAIPsParameters criteria;

    /**
     * Task to do on each AIP
     */
    private AbstractAIPUpdateTaskDto updateTask;

    public SearchAIPsParameters getCriteria() {
        return criteria;
    }

    public void setCriteria(SearchAIPsParameters criteria) {
        this.criteria = criteria;
    }

    public AbstractAIPUpdateTaskDto getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(AbstractAIPUpdateTaskDto updateTask) {
        this.updateTask = updateTask;
    }

    public static AIPUpdateParameters build(SearchAIPsParameters aipsParameters, AbstractAIPUpdateTaskDto updateTask) {
        AIPUpdateParameters params = new AIPUpdateParameters();
        params.setCriteria(aipsParameters);
        params.setUpdateTask(updateTask);
        return params;
    }
}
