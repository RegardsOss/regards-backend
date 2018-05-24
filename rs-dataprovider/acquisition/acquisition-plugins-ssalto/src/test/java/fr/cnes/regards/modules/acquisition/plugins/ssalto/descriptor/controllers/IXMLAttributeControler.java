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

import org.jdom.Element;
import org.jdom.Namespace;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;

/**
 * Interface des attributs du modèle
 * 
 * @author Christophe Mertz
 */
public interface IXMLAttributeControler {

    /**
     * 
     * Cette méthode permet de créer un élement XML à partir du POJO de l'attribut
     * 
     * @param attribute
     * @return l'attribut au format XML
     */
    public Element getElement(Attribute attribute);

    /**
     * 
     * Cette méthode permet de créer un élement XML à partir du POJO de l'attribut
     * 
     * @param attribute
     * @param xmlns
     * @return l'attribut au format XML
     */
    public Element getElement(Attribute attribute, Namespace xmlns);

    /**
     * Cette methode doit retourner le nom de l'element XML associe a l'objet
     * 
     * @return Le nom de l'element XML
     */
    public String doGetXmlElement();

    /**
     * Cette methode doit retourner une presentation String de l'objet pValue
     * 
     * @param value
     *            La valeur courante
     * @return La valeur courante transformee en string et inclue dans un Element
     */
    public Element doGetValueAsString(Object value);
}
