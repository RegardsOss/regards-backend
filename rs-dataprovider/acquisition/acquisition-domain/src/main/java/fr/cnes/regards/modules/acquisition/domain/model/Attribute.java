/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
public class Attribute {

    /**
     * Liste de valeurs de l'attribut.
     */
    protected List<Object> valueList = null;

    /**
     * Meta attribut definissant l'attribut
     */
    protected MetaAttribute metaAttribute = null;

    /**
     * L'instance de l'attribut compose dont fait partie l'attribut, il est null si l'attribut ne fait partie d'aucun
     * attribut compose
     */
    protected CompositeAttribute compositeAttribute = null;

    /**
     * Constructeur par défaut
     */
    protected Attribute(MetaAttribute pMetaAttribute) {
        super();
        metaAttribute = pMetaAttribute;
        valueList = new ArrayList<>();
    }

    /**
     * Cette methode existe uniquement pour pouvoir facilement acceder au type de l'attribut.
     *
     * @return Le type de l'attribut.
     */
    public AttributeTypeEnum getType() {
        return metaAttribute.getValueType();
    }

    /**
     * Retourne la liste de valeurs de l'attribut.
     *
     * @return Une List
     */
    public List<Object> getValueList() {
        return valueList;
    }

    /**
     * Ajoute une valeur a l'attribut. La classe de l'objet en entree doit correspondre avec la classe de l'attribut.
     *
     * @param pValue
     *            La nouvelle valeur de l'attribut.
     */
    public void addValue(Object pValue) {
        valueList.add(pValue);
    }

    /**
     * @return le méta attribut
     */
    public MetaAttribute getMetaAttribute() {
        return metaAttribute;
    }

    /**
     * @param pAttribute positionne
     *            le méta attribut
     */
    public void setMetaAttribute(MetaAttribute pAttribute) {
        metaAttribute = pAttribute;
    }

    /**
     * Cette methode permet de ajouter le nom du meta-attribut definissant l'attribut non-standard. La methode est
     * utilisee lors de l'ingestion. Cette methode ne fonctionnera pas sur un plate-forme Windows.
     *
     * @param pString
     *            Le chemin xml contenant le nom de l'attribut non-standard.
     */
    public void setMetaAttribute(String pString) {
        // processing path "XXXXX/XXXXX/XXXXX/FINAL" to get only final name "FINAL"
        File file = new File(pString);
        metaAttribute.setName(file.getName());
    }

    /**
     * Cette methode permet de mettre a jour le nom du meta-attribut definissant l'objet attribut. Cette methode est
     * utilisee lors de la lecture des fichiers descriptor.
     *
     * @param pString
     *            Le nom du meta-attribut.
     */
    public void setMetaAttributeName(String pString) {
        metaAttribute.setName(pString);
    }

    public CompositeAttribute getCompositeAttribute() {
        return this.compositeAttribute;
    }

    public void setCompositeAttribute(CompositeAttribute pCompositeAttribute) {
        this.compositeAttribute = pCompositeAttribute;
    }

    /**
     * renvoie une cle unique identifiant un meta-attribut en tenant compte de son appartenance a un attribut compose
     * Utilise pour les ajout aux hashmaps.
     *
     * @return une cle
     */
    public String getAttributeKey() {
        if (compositeAttribute == null) {
            return metaAttribute.getName();
        } else {
            StringBuilder buff = new StringBuilder(metaAttribute.getName());
            buff.append(" ");
            buff.append(compositeAttribute.getCompAttId());
            return buff.toString();
        }
    }

    /**
     * Ecrit le contenu de l'attribut Réservé au mode debug
     *
     * @return une representation de l'attribut
     */
    @Override
    public String toString() {
        StringBuilder localBuffer = new StringBuilder();
        localBuffer.append("[");
        if (metaAttribute != null) {
            localBuffer.append(metaAttribute.getName());
        }
        localBuffer.append(":");
        localBuffer.append(getType().toString());
        if (metaAttribute.getComputationRule() != null) {
            localBuffer.append(":");
            localBuffer.append(metaAttribute.getComputationRule());
        }
        localBuffer.append(":{");
        if (valueList != null && !valueList.isEmpty()) { // NOSONAR
            for (Object o : valueList) {
                localBuffer.append(o);
            }
        } else {
            localBuffer.append(" none...");
        }
        localBuffer.append("}]");
        return localBuffer.toString();
    }

    /**
     * Set method.
     *
     * @param pValueList
     *            the valueList to set
     */
    @SuppressWarnings("unchecked")
    public void setValueList(List<?> pValueList) {
        valueList = (List<Object>) pValueList;
    }
}