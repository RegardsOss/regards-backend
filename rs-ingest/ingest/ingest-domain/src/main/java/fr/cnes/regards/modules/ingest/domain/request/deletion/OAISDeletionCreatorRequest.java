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
package fr.cnes.regards.modules.ingest.domain.request.deletion;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.time.OffsetDateTime;

/**
 * Macro request that keeps info about a "massive" suppression of OAIS entities
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Entity(name = RequestTypeConstant.OAIS_DELETION_CREATOR_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class OAISDeletionCreatorRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb")
    private OAISDeletionCreatorPayload config;

    public static OAISDeletionCreatorRequest build(OAISDeletionCreatorPayload deletionPayload) {
        OAISDeletionCreatorRequest request = new OAISDeletionCreatorRequest();
        request.config = deletionPayload;
        request.setCreationDate(OffsetDateTime.now());
        request.setDtype(RequestTypeConstant.OAIS_DELETION_CREATOR_VALUE);
        request.setState(InternalRequestState.TO_SCHEDULE);
        //FIXME: what the hell is this??? config and request have similar attributes, which one is supposed to be the right one?????
        //FIXME: by the way this is the third object created for a REST request, if think less object and transformation would only help avoiding missing attributes!
        //FIXME: check if following is really done how it should be
        //FIXME: how to handle a simple request on multiple products belonging to multiple sessions?
        request.setSession(deletionPayload.getSession());
        request.setSessionOwner(deletionPayload.getSessionOwner());
        return request;
    }

    public OAISDeletionCreatorRequest() {
        this.config = new OAISDeletionCreatorPayload();
    }

    @Override
    public void setSessionOwner(String sessionOwner) {
        super.setSessionOwner(sessionOwner);
        config.setSessionOwner(sessionOwner);
    }

    @Override
    public void setSession(String session) {
        super.setSession(session);
        config.setSession(session);
    }

    public Boolean getDeletePhysicalFiles() {
        return config.getDeletePhysicalFiles();
    }

    public void setDeletePhysicalFiles(Boolean deletePhysicalFiles) {
        config.setDeletePhysicalFiles(deletePhysicalFiles);
    }

    public SessionDeletionMode getDeletionMode() {
        return config.getDeletionMode();
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        config.setDeletionMode(deletionMode);
    }

    public OAISDeletionCreatorPayload getConfig() {
        return config;
    }
}
