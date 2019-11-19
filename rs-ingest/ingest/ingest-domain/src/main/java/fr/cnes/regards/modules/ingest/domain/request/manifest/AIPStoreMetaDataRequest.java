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
package fr.cnes.regards.modules.ingest.domain.request.manifest;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import java.time.OffsetDateTime;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Storing info that a metadata should be saved on storage
 * @author Léo Mieulet
 */
@Entity(name = RequestTypeConstant.STORE_METADATA_VALUE)
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
public class AIPStoreMetaDataRequest extends AbstractRequest {

    /**
     * AIP to update
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb")
    private AIPStoreMetaDataPayload config;

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public AIPStoreMetaDataPayload getConfig() {
        return config;
    }

    public void setConfig(AIPStoreMetaDataPayload config) {
        this.config = config;
    }


    public boolean isRemoveCurrentMetaData() {
        return config.isRemoveCurrentMetaData();
    }

    public void setRemoveCurrentMetaData(boolean removeCurrentMetaData) {
        config.setRemoveCurrentMetaData(removeCurrentMetaData);
    }

    public boolean isComputeChecksum() {
        return config.isComputeChecksum();
    }

    public void setComputeChecksum(boolean computeChecksum) {
        config.setComputeChecksum(computeChecksum);
    }

    public Set<StoreLocation> getStoreLocations() {
        return config.getStoreLocations();
    }

    public void setStoreLocations(Set<StoreLocation> storages) {
        config.setStoreLocations(storages);
    }

    public static AIPStoreMetaDataRequest build(AIPEntity aip, Set<StoreLocation> storeLocations,
            boolean removeCurrentMetaData, boolean computeChecksum) {
        AIPStoreMetaDataRequest smdr = new AIPStoreMetaDataRequest();
        smdr.setState(InternalRequestStep.CREATED);
        smdr.setAip(aip);
        smdr.setSessionOwner(aip.getSessionOwner());
        smdr.setSession(aip.getSession());
        smdr.setProviderId(aip.getProviderId());
        smdr.setCreationDate(OffsetDateTime.now());
        smdr.setConfig(AIPStoreMetaDataPayload.build(storeLocations, removeCurrentMetaData, computeChecksum));
        return smdr;
    }
}
