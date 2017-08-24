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
package fr.cnes.regards.modules.acquisition.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe represente un bloc d'attributs: c'est à dire une instance d'atribut compose. Un attribut compose peut
 * etre n fois definie au niveau d'une entite cette classe correspond a un bloc definissant l'attribut compose. Un
 * identifiant permet de distinguer les différentes instances d'un même méta-attribut composé au niveau d'une entité.
 * 
 * @author Christophe Mertz
 *
 */
public class CompositeAttribute {

    /**
     * Le nom de l'attribut compose
     */
    private String name = null;

    /**
     * Liste des attributs.
     */
    private List<Attribute> attributeList = null;

    /**
     * Identifie de facon unique un attribut compose <b>pour une entite donnee</b>.
     */
    private int compAttId = 0;

    /**
     * Default constructor
     */
    public CompositeAttribute() {
        super();
        attributeList = new ArrayList<>();
    }

    public List<Attribute> getAttributeList() {
        return this.attributeList;
    }

    public void setAttributeList(List<Attribute> pAttributeList) {
        this.attributeList = pAttributeList;
    }

    public int getCompAttId() {
        return this.compAttId;
    }

    public void setCompAttId(int pInstanciationId) {
        this.compAttId = pInstanciationId;
    }

    /**
     * Ajoute un attribut a la liste d'attribut de l'attribut compose
     * 
     * @param pAttribute
     *            l'attribut a ajouter
     */
    public void addAttribute(Attribute pAttribute) {
        attributeList.add(pAttribute);
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }
}
