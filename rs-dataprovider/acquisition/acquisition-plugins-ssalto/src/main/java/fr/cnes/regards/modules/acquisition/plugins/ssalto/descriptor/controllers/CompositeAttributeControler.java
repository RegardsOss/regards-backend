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
import org.jdom.Namespace;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;



/**
 * classe permettant de parser les fichiers descripteurs
 *
 * @author Christophe Mertz
 */

public class CompositeAttributeControler extends AttributeControler {

    public CompositeAttributeControler() {
        super();
    }

    @Override
    public org.jdom.Element doGetValueAsString(Object pValue) {
        return null;
    }

    @Override
    public String doGetXmlElement() {
        return null;
    }

    @Override
    public org.jdom.Element getElement(Attribute pAttribute) {
        org.jdom.Element element = null;
        if (pAttribute instanceof CompositeAttribute) {
            CompositeAttribute attribute = (CompositeAttribute) pAttribute;
            element = new org.jdom.Element(attribute.getName());

            for (Attribute anotherAttribute : attribute.getAttributeList()) {
                for (Object value : anotherAttribute.getValueList()) {
                    org.jdom.Element attElement = new org.jdom.Element(anotherAttribute.getMetaAttribute().getName());
                    String val = SipadControlers.getControler(anotherAttribute).doGetStringValue(value);
                    attElement.addContent(val);
                    // add attribute element to the composite root element
                    element.addContent(attElement);
                }
            }
        }
        return element;
    }

    @Override
    public org.jdom.Element getElement(Attribute attr, Namespace xmlns) {
        org.jdom.Element element = null;
        if (attr instanceof CompositeAttribute) {
            CompositeAttribute attribute = (CompositeAttribute) attr;

            element = new org.jdom.Element(attribute.getName(), xmlns);

            if (attribute.getAttributeList() != null) {

                for (Attribute anotherAttribute : attribute.getAttributeList()) {
                    for (Object value : anotherAttribute.getValueList()) {
                        org.jdom.Element attElement = new org.jdom.Element(anotherAttribute.getMetaAttribute()
                                .getName(), xmlns);
                        String val = SipadControlers.getControler(anotherAttribute).doGetStringValue(value);
                        attElement.addContent(val);
                        // add attribute element to the composite root element
                        element.addContent(attElement);
                    }
                }
            }
        }
        return element;
    }

    /**
     * cree le bloc de l'attribut compose. si l'attribut compose n'a pas de nom, alors les bloc des sous attributs sont
     * ajoutes directement dans le parentElement
     *
     * @param parentElement
     * @param newDoc
     */
    public static void createAttributeElement(CompositeAttribute compositeAttribute, Element parentElement,
            DocumentImpl newDoc) {
        Element compositeRootElement = null;
        String name = compositeAttribute.getName();

        if (name != null) {
            // create composite root element
            compositeRootElement = newDoc.createElement(name);
        }
        for (Attribute anotherAttribute : compositeAttribute.getAttributeList()) {
            for (Object value : anotherAttribute.getValueList()) {
                Element attElement = newDoc.createElement(anotherAttribute.getMetaAttribute().getName());
                String val = SipadControlers.getControler(anotherAttribute).doGetStringValue(value);
                attElement.appendChild(newDoc.createTextNode(val));
                if (name != null) {
                    // add attribute element to the composite root element
                    compositeRootElement.appendChild(attElement);
                }
                else {
                    // add attribute element directly into parentElement
                    parentElement.appendChild(attElement);
                }
            }
            if (name != null) {
                parentElement.appendChild(compositeRootElement);
            }
        }
    }
}
