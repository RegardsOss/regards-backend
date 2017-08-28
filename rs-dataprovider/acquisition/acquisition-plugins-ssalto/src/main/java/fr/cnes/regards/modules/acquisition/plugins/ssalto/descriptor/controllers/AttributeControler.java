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
import org.jdom.Namespace;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.DescriptorException;

/**
 * Actions des attributs
 * 
 * @author Christophe Mertz
 */
public abstract class AttributeControler implements IXMLAttributeControler {

    /**
     * L'element XML designant le nom de l'attribut.
     */
    public static final String XML_ELEMENT_NAME = "name";

    /**
     * L'element XML designant une valeure de l'attribut.
     */
    public static final String XML_ELEMENT_VALUE = "value";

    public AttributeControler() {
        super();
    }

    @Override
    public Element getElement(Attribute pAttribute) {
        // Create root element
        Element attribute = new Element(doGetXmlElement());

        // Add the name of the attribute
        Element name = new Element(XML_ELEMENT_NAME);
        name.addContent(pAttribute.getMetaAttribute().getName());
        attribute.addContent(name);

        // add composite attribute information if any
        if (pAttribute.getCompositeAttribute() != null) {
            Element el = SsaltoControlers.getControler(pAttribute.getCompositeAttribute())
                    .getElement(pAttribute.getCompositeAttribute());
            attribute.addContent(el);
        }

        // Add the values of the attributes
        for (Object val : pAttribute.getValueList()) {
            attribute.addContent(doGetValueAsString(val));
        }

        return attribute;
    }

    @Override
    public Element getElement(Attribute pAttribute, Namespace pXmlns) {
        // Create root element
        Element attribute = new Element(doGetXmlElement(), pXmlns);

        // Add the name of the attribute
        Element name = new Element(XML_ELEMENT_NAME, pXmlns);
        name.addContent(pAttribute.getMetaAttribute().getName());
        attribute.addContent(name);

        // add composite attribute information if any
        if (pAttribute.getCompositeAttribute() != null) {
            Element el = SsaltoControlers.getControler(pAttribute.getCompositeAttribute())
                    .getElement(pAttribute.getCompositeAttribute());
            attribute.addContent(el);
        }

        // Add the values of the attributes
        for (Object val : pAttribute.getValueList()) {
            attribute.addContent(doGetValueAsString(val));
        }

        return attribute;
    }

    /**
     * renvoie une representation de type String d'un objet pValue
     * 
     * @param pValue
     * @return
     */
    public String doGetStringValue(Object pValue) {
        return pValue.toString();
    }

    /**
     * Cette methode valide l'index passe en parametre par rapport a la taille de la liste.
     * 
     * @param pIndex
     *            L'index que l'on veut verifier.
     * @throws DomainModelException
     *             L'index n'est pas valide
     */
    protected static void validate(Attribute pAttribute, int pIndex) throws ModuleException {
        if (pIndex < 0 || pIndex > pAttribute.getValueList().size()) {
            String msg = String.format(
                                       "The '%d' index passes as parameters is out of bounds for the values of the attribute",
                                       pIndex);
            throw new DescriptorException(msg);
        }
    }

    /**
     * Retourne la valeur de l'attribut.
     * 
     * @return La valeur de l'attribut.
     * @throws ModuleException
     *             L'index n'est pas valide
     */
    protected static Object getObjectValue(Attribute pAttribute, int pIndex) throws ModuleException {
        validate(pAttribute, pIndex);
        return pAttribute.getValueList().get(pIndex);
    }

    //    /**
    //     * Met a jour la valeur de l'attribut.
    //     * 
    //     * @param pValue
    //     *            La nouvelle valeur.
    //     * @throws DomainModelException
    //     *             L'index n'est pas valide.
    //     * @since 1.0
    //     */
    //    protected void setValue(Attribute pAttribute, Object pValue, int index) throws ModuleException {
    //        validate(pAttribute, index);
    //        pAttribute.getValueList().set(index, pValue);
    //    }
}
