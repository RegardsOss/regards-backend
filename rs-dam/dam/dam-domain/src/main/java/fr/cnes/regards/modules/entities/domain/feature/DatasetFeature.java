/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.entities.domain.feature;

import java.util.UUID;

import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * Specific feature properties for datasets
 * @author Marc Sordi
 *
 */
public class DatasetFeature extends EntityFeature {

    /**
     * Licence
     */
    private String licence;

    /**
     * Deserialization constructor
     */
    protected DatasetFeature() {
        super(null, null, EntityType.DATASET, null);
    }

    public DatasetFeature(String tenant, String providerId, String label) {
        super(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, tenant, UUID.randomUUID(), 1), providerId,
              EntityType.DATASET, label);
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }
}
