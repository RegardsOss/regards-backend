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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * 
 * @author Christophe Mertz
 */
public interface IEntityDescriptorElementControler<T extends EntityDescriptorElement> {

    /**
     * renvoie une representation xml de l'element de description
     * 
     * @param newDoc
     *            le document a construire
     * @return Element le bloc a inserer dans le document
     */
    public Element getElement(T entityDescriptorElement, DocumentImpl newDoc);

    /**
     * merge deux elements de descriptions
     * 
     * @param descriptorElement
     */
    public void merge(T entityDescriptorElement, T descriptorElement);
}
