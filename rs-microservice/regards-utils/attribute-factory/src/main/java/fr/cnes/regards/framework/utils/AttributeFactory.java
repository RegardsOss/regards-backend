/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils;

import fr.cnes.regards.framework.utils.metamodel.MetaAttribute;
import fr.cnes.regards.framework.utils.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility class to manage attribut's types
 *
 * @author Christophe Mertz
 */
public final class AttributeFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFactory.class);

    /**
     * Non public constructor. Utility class that should not be instanciated.
     */
    private AttributeFactory() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     *
     * @param type                   Le type de l'attribute - enumere.
     * @param label                  Le libelle de l'attribute.
     * @param compositeAttributeName le nom de l'attribut compose dont fait partie l'attribut (null s'il ne fait partie d'aucun attribut
     *                               compose)
     * @param blockId                l'identifiant du bloc de l'attribut compose de l'entite
     * @return Un objet <code>Attribute</code> de type non standard.
     */
    public static Attribute createAttribute(AttributeTypeEnum type,
                                            String label,
                                            String compositeAttributeName,
                                            int blockId) {

        Attribute attribute = null;
        // According to the type, create either a real, string, date or integer attribute
        switch (type) {
            case TYPE_REAL:
                attribute = new RealAttribute();
                break;
            case TYPE_INTEGER:
                attribute = new LongAttribute();
                break;
            case TYPE_DATE_TIME:
                attribute = new DateTimeAttribute();
                break;
            case TYPE_DATE:
                attribute = new DateAttribute();
                break;
            case TYPE_STRING:
                attribute = new StringAttribute();
                break;
            case TYPE_URL:
                attribute = new UrlAttribute();
                break;
            case TYPE_LONG_STRING:
            case TYPE_CLOB:
                attribute = new ClobAttribute();
                break;
            case TYPE_GEO_LOCATION:
                attribute = new GeoAttribute();
                break;
            default:
                LOGGER.error("Type inconnu : " + type.getTypeName());
                return null;
        }

        MetaAttribute meta = new MetaAttribute();
        meta.setName(label);
        meta.setValueType(type); // FA-ID : SIPNG-FA-0121-CN : Line added
        attribute.setMetaAttribute(meta);

        if (compositeAttributeName != null) {
            // build the composite attribute which this attribut is part of
            CompositeAttribute block = new CompositeAttribute();
            block.setName(compositeAttributeName);
            block.setCompAttId(blockId);
            attribute.setCompositeAttribute(block);
        }

        return attribute;
    }

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     *
     * @param type      Le type de l'attribut.
     * @param label     Le libelle de l'attribut.
     * @param valueList La liste de valeurs de l'attribut.
     * @return Un objet <code>Attribute</code> de type non standard.
     */
    public static Attribute createAttribute(AttributeTypeEnum type, String label, List<?> valueList) {

        // According to the type, create either a real, string, date or integer attribute
        Attribute attribute = createAttribute(type, label, null, 0);

        if (attribute != null) {
            attribute.setValueList(valueList);
        }

        return attribute;
    }

    /**
     * Cette methode permet de creer un attribut a partir de son type, de son nom et de son identifiant.
     *
     * @param type                   Le type de l'attribut.
     * @param label                  Le libelle de l'attribut.
     * @param value                  la valeur de l'attribut
     * @param compositeAttributeName le nom de l'attribut compose dont fait partie l'attribut (null s'il ne fait partie d'aucun attribut
     *                               compose)
     * @param blockId                l'identifiant du bloc de l'attribut compose de l'entite
     * @return Un objet <code>Attribute</code> de type non standard.
     */
    public static Attribute createAttribute(AttributeTypeEnum type,
                                            String label,
                                            Object value,
                                            String compositeAttributeName,
                                            int blockId) {

        // According to the type, create either a real, string, date or integer attribute
        Attribute attribute = createAttribute(type, label, compositeAttributeName, blockId);

        if (attribute != null) {
            attribute.addValue(value);
        }

        return attribute;
    }
}
