/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import fr.cnes.regards.modules.acquisition.domain.metamodel.MetaAttribute;

/**
 * Cette classe represente une date dont la precision est a la milliseconde
 * 
 * @author Christophe Mertz
 *
 */
public class DateTimeAttribute extends Attribute {

    /**
     * Constructor
     */
    public DateTimeAttribute() {
        super(new MetaAttribute(AttributeTypeEnum.TYPE_DATE_TIME));
    }

    /**
     * Ajoute une valeur a l'attribut La classe de l'objet en entree doit correspondre avec la classe de l'attribut
     * 
     * @param value
     *            La nouvelle valeur de l'attribut
     */
    public void addValue(Long value) {
        Date date = new Date(value);
        super.addValue(OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
    }
}
