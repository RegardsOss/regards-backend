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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.jdom.Element;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;

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

    private final static DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public DateTimeAttributeControler() {
        super();
    }

    @Override
    public String doGetXmlElement() {
        return XML_ELEMENT;
    }

    @Override
    public Element doGetValueAsString(Object strValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        value.addContent(String.valueOf(((Date) strValue).getTime()));
        return value;
    }

    /**
     * renvoie une representation de type String d'un objet pValue
     * 
     * @param value
     * @return
     */
    @Override
    public String doGetStringValue(Object value) {
        return DATETIME_FORMATTER.format((OffsetDateTime) value);
    }

    /**
     * Retourne la valeur de l'attribut
     * @param attribut
     * @param index
     * @return la valeur de l'attribut
     * @throws ModuleException
     */
    public Date getValue(DateTimeAttribute attribut, int index) throws ModuleException {
        // TODO CMZ à revoir        
        return (Date) getObjectValue(attribut, index);
    }
}
