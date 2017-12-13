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

import org.jdom.Element;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.LongAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;

/**
 * Attribut long
 * 
 * @author Christophe Mertz
 */
public class LongAttributeControler extends AttributeControler {

    /**
     * Nom de l'element XML equivalent a l'objet.
     */
    public static final String XML_ELEMENT = "longAttribute";

    public LongAttributeControler() {
        super();
    }

    @Override
    public String doGetXmlElement() {
        return XML_ELEMENT;
    }

    @Override
    public Element doGetValueAsString(Object pValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        value.addContent(String.valueOf(pValue));
        return value;
    }

    /**
     * Retourne la valeur de l'attribut.
     * 
     * @return La valeur de l'attribut.
     * @see sipad.client.common.domain.model.LightAttribute#getValue()
     * @throws DomainModelException
     *             l'index n'est pas valide
     */
    public static Long getValue(LongAttribute pAttribut, int index) throws ModuleException {
        return (Long) getObjectValue(pAttribut, index);
    }
}
