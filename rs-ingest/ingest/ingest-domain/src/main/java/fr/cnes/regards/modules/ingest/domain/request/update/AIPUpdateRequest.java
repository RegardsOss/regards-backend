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
package fr.cnes.regards.modules.ingest.domain.request.update;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractInternalRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Keep info about an AIP update request
 * @author Léo Mieulet
 */
@Entity(name = "AIPUpdateRequest")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateRequest extends AbstractInternalRequest {


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "update_task_id", foreignKey = @ForeignKey(name = "fk_update_request_update_task_id"))
    @Valid
    private AbstractAIPUpdateTask updateTask;

    /**
     * AIP to update
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    public AbstractAIPUpdateTask getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(AbstractAIPUpdateTask updateTask) {
        this.updateTask = updateTask;
    }

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public static List<AIPUpdateRequest> build(AIPEntity aip, AIPUpdateParametersDto updateTaskDto, boolean pending) {
        return AIPUpdateRequest.build(aip, AbstractAIPUpdateTask.build(updateTaskDto), pending);
    }

    public static List<AIPUpdateRequest> build(AIPEntity aip, List<AbstractAIPUpdateTask> updateTasks, boolean pending) {
        List<AIPUpdateRequest> result = new ArrayList<>();
        for (AbstractAIPUpdateTask updateTask : updateTasks) {
            AIPUpdateRequest updateRequest = new AIPUpdateRequest();
            updateRequest.setUpdateTask(updateTask);
            updateRequest.setAip(aip);
            updateRequest.setCreationDate(OffsetDateTime.now());
            updateRequest.setSessionOwner(aip.getIngestMetadata().getSessionOwner());
            updateRequest.setSession(aip.getIngestMetadata().getSession());
            updateRequest.setProviderId(aip.getProviderId());
            if (pending) {
                updateRequest.setState(InternalRequestStep.BLOCKED);
            } else {
                updateRequest.setState(InternalRequestStep.CREATED);
            }
            result.add(updateRequest);
        }
        return result;
    }
}
