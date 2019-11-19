/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Macro request that keeps info about a "massive" suppression of OAIS entities
 * @author Marc SORDI
 */
@Entity(name = RequestTypeConstant.OAIS_DELETION_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class OAISDeletionRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb")
    private OAISDeletionPayload config;

    public OAISDeletionRequest() {
        this.config = new OAISDeletionPayload();
    }

    public OAISDeletionPayload getConfig() {
        return config;
    }

    public void setConfig(OAISDeletionPayload config) {
        this.config = config;
    }


    public SessionDeletionMode getDeletionMode() {
        return config.getDeletionMode();
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        config.setDeletionMode(deletionMode);
    }

    public SessionDeletionSelectionMode getSelectionMode() {
        return config.getSelectionMode();
    }

    public void setSelectionMode(SessionDeletionSelectionMode selectionMode) {
        config.setSelectionMode(selectionMode);
    }

    public Boolean getDeletePhysicalFiles() {
        return config.getDeletePhysicalFiles();
    }

    public void setDeletePhysicalFiles(Boolean deletePhysicalFiles) {
        config.setDeletePhysicalFiles(deletePhysicalFiles);
    }

    public Set<String> getSipIds() {
        return config.getSipIds();
    }

    public void setSipIds(Set<String> sipIds) {
        config.setSipIds(sipIds);
    }

    public Set<String> getProviderIds() {
        return config.getProviderIds();
    }

    public void setProviderIds(Set<String> providerIds) {
        config.setProviderIds(providerIds);
    }
}
