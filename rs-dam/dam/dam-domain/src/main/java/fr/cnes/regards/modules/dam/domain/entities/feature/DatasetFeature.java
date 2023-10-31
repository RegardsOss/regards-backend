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
package fr.cnes.regards.modules.dam.domain.entities.feature;

import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

import java.util.UUID;

/**
 * Specific feature properties for datasets
 *
 * @author Marc Sordi
 */
public class DatasetFeature extends EntityFeature {

    /**
     * Licence
     */
    private String licence;

    private Boolean dataObjectsFilesAccessGranted = false;

    private Boolean dataObjectsAccessGranted = false;

    /**
     * Deserialization constructor
     */
    protected DatasetFeature() {
        super(null, null, EntityType.DATASET, null);
    }

    public DatasetFeature(String tenant, String providerId, String label) {
        super(new OaisUniformResourceName(OAISIdentifier.AIP,
                                          EntityType.DATASET,
                                          tenant,
                                          UUID.randomUUID(),
                                          1,
                                          null,
                                          null), providerId, EntityType.DATASET, label);
    }

    public DatasetFeature(UniformResourceName id, String providerId, String label, String licence) {
        super(id, providerId, EntityType.DATASET, label);
        this.licence = licence;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public Boolean getDataObjectsFilesAccessGranted() {
        return dataObjectsFilesAccessGranted;
    }

    public void setDataObjectsFilesAccessGranted(Boolean dataObjectsFilesAccessGranted) {
        this.dataObjectsFilesAccessGranted = dataObjectsFilesAccessGranted;
    }

    public Boolean getDataObjectsAccessGranted() {
        return dataObjectsAccessGranted;
    }

    public void setDataObjectsAccessGranted(Boolean dataObjectsAccessGranted) {
        this.dataObjectsAccessGranted = dataObjectsAccessGranted;
    }
}
