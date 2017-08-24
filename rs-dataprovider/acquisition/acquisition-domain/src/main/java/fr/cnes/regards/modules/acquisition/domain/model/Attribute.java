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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.modules.acquisition.domain.metamodel.MetaAttribute;


/**
 * Cette interface permet d'abstraire la relation d'une entite avec la valeur d'un attribut qui la compose.
 * 
 * @author Christophe Mertz
 *
 */
public abstract class Attribute {

    /**
     * Liste de valeurs de l'attribut.
     * 
     * @since 1.0
     */
    protected List<Object> valueList_ = null;

    /**
     * Meta attribut definissant l'attribut
     * 
     * @since 1.0
     */
    protected MetaAttribute metaAttribute_ = null;

    /**
     * L'instance de l'attribut compose dont fait partie l'attribut, il est null si l'attribut ne fait partie d'aucun
     * attribut compose
     * 
     * @since 3.0
     */
    protected CompositeAttribute compositeAttribute_ = null;

    /**
     * Constructeur par défaut
     * 
     * @since 1.0
     */
    protected Attribute(MetaAttribute pMetaAttribute) {
        super();
        metaAttribute_ = pMetaAttribute;
        valueList_ = new ArrayList<>();
    }

    /**
     * Cette methode existe uniquement pour pouvoir facilement acceder au type de l'attribut.
     * 
     * @return Le type de l'attribut.
     * @since 1.0
     */
    public AttributeTypeEnum getType() {
        return metaAttribute_.getValueType();
    }

    /**
     * Retourne la liste de valeurs de l'attribut.
     * 
     * @return Une List
     * @since 1.0
     */
    public List<Object> getValueList() {
        return valueList_;
    }

    /**
     * Ajoute une valeur a l'attribut. La classe de l'objet en entree doit correspondre avec la classe de l'attribut.
     * 
     * @param pValue
     *            La nouvelle valeur de l'attribut.
     * @since 1.0
     * @DM SIPNG-DM-012-CN : changement de visibilite
     */
    public void addValue(Object pValue) {
        valueList_.add(pValue);
    }

    /**
     * @return le méta attribut
     * @since 5.1
     */
    public MetaAttribute getMetaAttribute() {
        return metaAttribute_;
    }

    /**
     * @param positionne
     *            le méta attribut
     * @since 5.1
     */
    public void setMetaAttribute(MetaAttribute pAttribute) {
        metaAttribute_ = pAttribute;
    }

    /**
     * Cette methode permet de ajouter le nom du meta-attribut definissant l'attribut non-standard. La methode est
     * utilisee lors de l'ingestion. Cette methode ne fonctionnera pas sur un plate-forme Windows.
     * 
     * @param pString
     *            Le chemin xml contenant le nom de l'attribut non-standard.
     * @since 1.0
     */
    public void setMetaAttribute(String pString) {
        // processing path "XXXXX/XXXXX/XXXXX/FINAL" to get only final name "FINAL"
        File file = new File(pString);
        metaAttribute_.setName(file.getName());
    }

    /**
     * Cette methode permet de mettre a jour le nom du meta-attribut definissant l'objet attribut. Cette methode est
     * utilisee lors de la lecture des fichiers descriptor.
     * 
     * @param pString
     *            Le nom du meta-attribut.
     * @since 1.0
     */
    public void setMetaAttributeName(String pString) {
        metaAttribute_.setName(pString);
    }

    public CompositeAttribute getCompositeAttribute() {
        return this.compositeAttribute_;
    }

    public void setCompositeAttribute(CompositeAttribute pCompositeAttribute) {
        this.compositeAttribute_ = pCompositeAttribute;
    }

    /**
     * renvoie une cle unique identifiant un meta-attribut en tenant compte de son appartenance a un attribut compose
     * Utilise pour les ajout aux hashmaps.
     * 
     * @return une cle
     * @since 3.0
     */
    public String getAttributeKey() {
        String key = metaAttribute_.getName();
        if (compositeAttribute_ != null) {
            // this is unique key as meta-attribut names doesn't contain space char
            key = key + " " + compositeAttribute_.getCompAttId();
        }
        return key;
    }

    /**
     * Ecrit le contenu de l'attribut Réservé au mode debug
     * 
     * @return une representation de l'attribut
     * @since 1.0
     */
    @Override
    public String toString() {
        StringBuffer localBuffer = new StringBuffer();
        localBuffer.append("Attribute description:");
        if (metaAttribute_ != null) {
            localBuffer.append(" label: " + metaAttribute_.getName());
        }
        localBuffer.append(" value(s):");
        if ((valueList_ != null) && !valueList_.isEmpty()) {
            for (Object o : valueList_) {
                localBuffer.append(" " + o);
            }
        }
        else {
            localBuffer.append(" none...");
        }
        return localBuffer.toString();
    }

    /**
     * Set method.
     * 
     * @param pValueList
     *            the valueList to set
     * @since 5.2
     */
    @SuppressWarnings("unchecked")
    public void setValueList(List<?> pValueList) {
        valueList_ = (List<Object>) pValueList;
    }
}