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
package fr.cnes.regards.modules.feature.domain.request;

import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

import java.util.List;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */

@Embeddable
@MappedSuperclass
public class FeatureMetadataEntity extends FeatureStorageMedataEntity {

    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    public static FeatureMetadataEntity build(String sessionOwner, String session, List<StorageMetadata> storages) {
        FeatureMetadataEntity f = new FeatureMetadataEntity();
        f.setSessionOwner(sessionOwner);
        f.setSession(session);
        f.setStorages(storages);
        return f;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

}
