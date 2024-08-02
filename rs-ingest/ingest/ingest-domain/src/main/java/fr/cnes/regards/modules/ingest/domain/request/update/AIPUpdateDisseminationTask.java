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
package fr.cnes.regards.modules.ingest.domain.request.update;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.util.List;

/**
 * @author Michael Nguyen
 */
@Entity(name = "UpdateDisseminationAIPTask")
public class AIPUpdateDisseminationTask extends AbstractAIPUpdateTask {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo") })
    private List<DisseminationInfo> disseminationInfoUpdates;

    public List<DisseminationInfo> getDisseminationInfoUpdates() {
        return disseminationInfoUpdates;
    }

    /**
     * List of the different dissemination info to update on aip
     */
    public void setDisseminationInfoUpdates(List<DisseminationInfo> disseminationInfoUpdates) {
        this.disseminationInfoUpdates = disseminationInfoUpdates;
    }

    public static AIPUpdateDisseminationTask build(AIPUpdateTaskType type, AIPUpdateState state) {
        AIPUpdateDisseminationTask task = new AIPUpdateDisseminationTask();
        task.setType(type);
        task.setState(state);
        return task;
    }

    public static AIPUpdateDisseminationTask build(AIPUpdateTaskType type,
                                                   AIPUpdateState state,
                                                   List<DisseminationInfo> disseminationInfos) {
        AIPUpdateDisseminationTask task = new AIPUpdateDisseminationTask();
        task.setType(type);
        task.setDisseminationInfoUpdates(disseminationInfos);
        task.setState(state);
        return task;
    }
}
