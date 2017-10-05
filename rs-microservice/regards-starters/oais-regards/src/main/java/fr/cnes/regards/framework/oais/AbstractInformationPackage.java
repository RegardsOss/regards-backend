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
package fr.cnes.regards.framework.oais;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 *
 * OAIS Information package base structure
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractInformationPackage {

    /**
     * Type of entity represented by this information package
     */
    protected EntityType type;

    /**
     * Tag list
     */
    protected List<String> tags;

    /**
     * List of Information Object
     */
    @NotNull
    protected InformationPackage informationPackage;

    public AbstractInformationPackage() {
        tags = new ArrayList<>();
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType pType) {
        type = pType;
    }

    public List<String> getTags() {
        return tags;
    }

    public InformationPackage getInformationPackage() {
        return informationPackage;
    }

    public void setInformationPackage(InformationPackage informationPackage) {
        this.informationPackage = informationPackage;
    }
}
