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
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.aip.OAISDateRange;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SearchSelectionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import java.time.OffsetDateTime;
import java.util.List;
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

    public static OAISDeletionRequest build(OAISDeletionPayload deletionPayload) {
        OAISDeletionRequest request = new OAISDeletionRequest();
        request.setConfig(deletionPayload);
        request.setCreationDate(OffsetDateTime.now());
        request.setState(InternalRequestState.RUNNING);
        return request;
    }

    public OAISDeletionRequest() {
        this.config = new OAISDeletionPayload();
    }

    public OAISDeletionPayload getConfig() {
        return config;
    }

    public void setConfig(OAISDeletionPayload config) {
        this.config = config;
    }

    public AIPState getAIPState() {
        return config.getState();
    }

    public void setAIPState(AIPState state) {
        config.setState(state);
    }

    public List<String> getTags() {
        return config.getTags();
    }

    public void setTags(List<String> tags) {
        config.setTags(tags);
    }

    public Set<String> getProviderIds() {
        return config.getProviderIds();
    }

    public void setProviderIds(Set<String> providerIds) {
        config.setProviderIds(providerIds);
    }

    public String getSessionOwner() {
        return config.getSessionOwner();
    }

    public void setSessionOwner(String sessionOwner) {
        config.setSessionOwner(sessionOwner);
    }

    public String getSession() {
        return config.getSession();
    }

    public void setSession(String session) {
        config.setSession(session);
    }

    public Set<String> getStorages() {
        return config.getStorages();
    }

    public void setStorages(Set<String> storages) {
        config.setStorages(storages);
    }

    public Set<String> getCategories() {
        return config.getCategories();
    }

    public void setCategories(Set<String> categories) {
        config.setCategories(categories);
    }

    public OAISDateRange getLastUpdate() {
        return config.getLastUpdate();
    }

    public void setLastUpdate(OAISDateRange lastUpdate) {
        config.setLastUpdate(lastUpdate);
    }

    public SearchSelectionMode getSelectionMode() {
        return config.getSelectionMode();
    }

    public void setSelectionMode(SearchSelectionMode selectionMode) {
        config.setSelectionMode(selectionMode);
    }

    public List<String> getAipIds() {
        return config.getAipIds();
    }

    public void setAipIds(List<String> aipIds) {
        config.setAipIds(aipIds);
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
}
