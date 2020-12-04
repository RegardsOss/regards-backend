/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.deletion;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Request to handle deletion of an OAIS product (sip/aips)
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 *
 */
@Entity(name = RequestTypeConstant.OAIS_DELETION_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class OAISDeletionRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private OAISDeletionPayload config;

    /**
     * AIP to delete
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    public static OAISDeletionRequest build(AIPEntity aipToDelete, SessionDeletionMode deletionMode,
            boolean deleteFiles) {
        OAISDeletionRequest odr = new OAISDeletionRequest();
        odr.aip = aipToDelete;
        odr.config = OAISDeletionPayload.build(deletionMode, deleteFiles);
        odr.setCreationDate(OffsetDateTime.now());
        odr.setSessionOwner(aipToDelete.getSessionOwner());
        odr.setSession(aipToDelete.getSession());
        odr.setProviderId(aipToDelete.getProviderId());
        odr.setDtype(RequestTypeConstant.OAIS_DELETION_VALUE);
        odr.setState(InternalRequestState.TO_SCHEDULE);
        return odr;
    }

    public DeletionRequestStep getStep() {
        return config.getStep();
    }

    public void setStep(DeletionRequestStep step) {
        config.setStep(step);
    }

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public void setAipToNotify(AIPEntity toNotify) {
        config.setAipToNotify(toNotify);
    }

    public AIPEntity getAipToNotify() {
        return config.getAipToNotify();
    }

    public boolean isDeleteFiles() {
        return config.isDeleteFiles();
    }

    public void setDeleteFiles(boolean deleteFiles) {
        config.setDeleteFiles(deleteFiles);
    }

    public SessionDeletionMode getDeletionMode() {
        return config.getDeletionMode();
    }

    public boolean isRequestFilesDeleted() {
        return config.isRequestFilesDeleted();
    }

    public void setRequestFilesDeleted() {
        config.setRequestFilesDeleted();
    }
}
