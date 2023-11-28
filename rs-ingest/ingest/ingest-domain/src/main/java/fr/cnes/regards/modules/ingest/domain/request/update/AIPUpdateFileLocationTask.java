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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.filecatalog.dto.request.RequestResultInfoDto;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.List;

/**
 * @author Léo Mieulet
 */
@Entity(name = "UpdateFileLocationAIPTask")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateFileLocationTask extends AbstractAIPUpdateTask {

    /**
     * File list that were either added or removed from a storage location
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb",
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.modules.filecatalog.dto.request.RequestResultInfoDto") })
    private List<RequestResultInfoDto> fileLocationUpdates;

    public List<RequestResultInfoDto> getFileLocationUpdates() {
        return fileLocationUpdates;
    }

    public void setFileLocationUpdates(List<RequestResultInfoDto> fileLocationUpdates) {
        this.fileLocationUpdates = fileLocationUpdates;
    }

    public static AIPUpdateFileLocationTask buildAddLocationTask(List<RequestResultInfoDto> fileLocationUpdates) {
        AIPUpdateFileLocationTask task = new AIPUpdateFileLocationTask();
        task.setType(AIPUpdateTaskType.ADD_FILE_LOCATION);
        task.setState(AIPUpdateState.READY);
        task.setFileLocationUpdates(fileLocationUpdates);
        return task;
    }

    public static AIPUpdateFileLocationTask buildRemoveLocationTask(List<RequestResultInfoDto> fileLocationUpdates) {
        AIPUpdateFileLocationTask task = new AIPUpdateFileLocationTask();
        task.setType(AIPUpdateTaskType.REMOVE_FILE_LOCATION);
        task.setState(AIPUpdateState.READY);
        task.setFileLocationUpdates(fileLocationUpdates);
        return task;
    }
}
