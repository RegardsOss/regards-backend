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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.metamodel.MetaAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;

/**
 * Classe pour creer les differents type d'attributs.
 * 
 * @author Christophe Mertz
 *
 */
public class AttributeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFactory.class);

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     * 
     * @param pType
     *            Le type de l'attribute - enumere.
     * @param pLabel
     *            Le libelle de l'attribute.
     * @param pCompositeAttributeName
     *            le nom de l'attribut compose dont fait partie l'attribut (null s'il ne fait partie d'aucun attribut
     *            compose)
     * @param pBlockId
     *            l'identifiant du bloc de l'attribut compose de l'entite
     * @return Un objet <code>Attribute</code> de type non standard.
     */
    public static Attribute createAttribute(AttributeTypeEnum pType, String pLabel, String pCompositeAttributeName,
            int pBlockId) {

        Attribute attribute = null;
        // According to the type, create either a real, string, date or integer attribute
        switch (pType) {
            case TYPE_REAL: {
                attribute = new RealAttribute();
                break;
            }
            case TYPE_INTEGER: {
                attribute = new LongAttribute();
                break;
            }
            case TYPE_DATE_TIME: {
                attribute = new DateTimeAttribute();
                break;
            }
            case TYPE_DATE: {
                attribute = new DateAttribute();
                break;
            }
            case TYPE_STRING: {
                attribute = new StringAttribute();
                break;
            }
            case TYPE_URL: {
                attribute = new UrlAttribute();
                break;
            }
            case TYPE_LONG_STRING:
            case TYPE_CLOB: {
                attribute = new ClobAttribute();
                break;
            }
            case TYPE_GEO_LOCATION: {
                attribute = new GeoAttribute();
                break;
            }
            default: {
                LOGGER.error("Type inconnu : " + pType.getTypeName());
                return null;
            }
        }

        MetaAttribute meta = new MetaAttribute();
        meta.setName(pLabel);
        meta.setValueType(pType); // FA-ID : SIPNG-FA-0121-CN : Line added
        meta.setIsStandard(Boolean.FALSE); // FA-ID : SIPNG-FA-0121-CN : Line added
        attribute.setMetaAttribute(meta);

        if (pCompositeAttributeName != null) {
            // build the composite attribute which this attribut is part of
            CompositeAttribute block = new CompositeAttribute();
            block.setName(pCompositeAttributeName);
            block.setCompAttId(pBlockId);
            attribute.setCompositeAttribute(block);
        }

        return attribute;
    }

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     * 
     * @param pType
     *            Le type de l'attribut.
     * @param pLabel
     *            Le libelle de l'attribut.
     * @param pValueList
     *            La liste de valeurs de l'attribut.
     * @return Un objet <code>Attribute</code> de type non standard.
     * @throws DomainModelException
     *             Exception survenue pendant le traitement
     */
    public static Attribute createAttribute(AttributeTypeEnum pType, String pLabel, List<?> pValueList)
            throws DomainModelException {

        // According to the type, create either a real, string, date or integer attribute
        Attribute attribute = createAttribute(pType, pLabel, null, 0);

        if (attribute != null) {
            attribute.setValueList(pValueList);
        }

        return attribute;
    }

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     * 
     * @param pType
     *            Le type de l'attribut.
     * @param pLabel
     *            Le libelle de l'attribut.
     * @param pValue
     *            la valeur de l'attribut
     * @param pCompositeAttributeName
     *            le nom de l'attribut compose dont fait partie l'attribut (null s'il ne fait partie d'aucun attribut
     *            compose)
     * @param pBlockId
     *            l'identifiant du bloc de l'attribut compose de l'entite
     * @return Un objet <code>Attribute</code> de type non standard.
     * @throws DomainModelException
     *             Exception survenue pendant le traitement
     */
    public static Attribute createAttribute(AttributeTypeEnum pType, String pLabel, Object pValue,
            String pCompositeAttributeName, int pBlockId) throws DomainModelException {

        // According to the type, create either a real, string, date or integer attribute
        Attribute attribute = createAttribute(pType, pLabel, pCompositeAttributeName, pBlockId);

        if (attribute != null) {
            attribute.addValue(pValue);
        }

        return attribute;
    }
}
