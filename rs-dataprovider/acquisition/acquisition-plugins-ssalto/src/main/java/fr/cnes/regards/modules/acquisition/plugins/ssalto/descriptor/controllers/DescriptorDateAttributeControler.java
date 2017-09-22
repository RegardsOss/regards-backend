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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorDateAttribute;

public class DescriptorDateAttributeControler extends DateAttributeControler {

    /**
     * Cette constante donne le format des dates utilisees dans les descriptors.
     */
    private static final String DATE_ATTRIBUTE_FORMAT = "yyyy/MM/dd";

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorDateAttributeControler.class);

    public DescriptorDateAttributeControler() {
        super();
    }

    /**
     * Ajoute une valeur a l'attribut. La chaine en entree doit etre du format yyyy/MM/dd
     * 
     * @param newVal
     *            La valeur a ajouter
     */
    public void addStringValue(DescriptorDateAttribute attr, String newVal) throws ModuleException {
        try {
            DateFormat parser = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
            parser.setLenient(false);
            attr.addValue(parser.parse(newVal));
        } catch (ParseException e) {
            String msg = String.format("The '%s' does not match the '%s' format", newVal, DATE_ATTRIBUTE_FORMAT);
            LOGGER.error(msg, e);
            throw new ModuleException(msg, e);
        }
    }

    @Override
    public Element doGetValueAsString(Object newVal) {
        Element value = new Element(XML_ELEMENT_VALUE);
        DateFormat formater = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
        value.addContent(formater.format((Date) newVal));
        return value;
    }
}
