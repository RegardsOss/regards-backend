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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * @author Christophe Mertz
 */
public abstract class EntityDescriptorElementControler
        implements IEntityDescriptorElementControler<EntityDescriptorElement> {

    /**
     * Le nom de l'attribut XML de creation d'un objet de stockage
     */
    protected static final String ENTITY_TYPE = "ENTITY_TYPE";

    @Override
    public abstract Element getElement(EntityDescriptorElement pElement, DocumentImpl pNewDoc);

    @Override
    public abstract void merge(EntityDescriptorElement pEntityDescriptorElement,
            EntityDescriptorElement pDescriptorElement);

}