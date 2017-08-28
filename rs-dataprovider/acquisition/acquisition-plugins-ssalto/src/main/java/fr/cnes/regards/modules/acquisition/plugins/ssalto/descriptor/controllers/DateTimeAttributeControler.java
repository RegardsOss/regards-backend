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

import java.util.Date;

import org.jdom.Element;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * Attribut de type DATE TIME
 * 
 * @author Christophe Mertz
 */
public class DateTimeAttributeControler extends AttributeControler {

    /**
     * Nom de l'element XML equivalent a l'objet.
     */
    public static final String XML_ELEMENT = "dateTimeAttribute";

    public DateTimeAttributeControler() {
        super();
    }

    @Override
    public String doGetXmlElement() {
        return XML_ELEMENT;
    }

    @Override
    public Element doGetValueAsString(Object pValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        value.addContent(String.valueOf(((Date) pValue).getTime()));
        return value;
    }

    /**
     * renvoie une representation de type String d'un objet pValue
     * 
     * @param pValue
     * @return
     */
    @Override
    public String doGetStringValue(Object pValue) {
        String outputValue;
        outputValue = String
                .valueOf(DateFormatter.getDateRepresentation((Date) pValue, DateFormatter.XS_DATE_TIME_FORMAT));
        return outputValue;
    }

    /**
     * Retourne la valeur de l'attribut.
     * 
     * @param pIndex
     * @return La valeur de l'attribut.
     * @throws DomainModelException
     *             l'index n'est pas valide
     */
    public Date getValue(DateTimeAttribute pAttribut, int pIndex) throws ModuleException {
        Date retour = (Date) getObjectValue(pAttribut, pIndex);
        return retour;
    }
}
