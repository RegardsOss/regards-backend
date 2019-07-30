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
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * Annotation allowing us to automatically add a class and it's subtypes to a {@link PolymorphicTypeAdapterFactory}
 * This annotation should be only used on abstract type.
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Gsonable {

    /**
     * Specify the discriminator field name for serialization and deserialization. If it is empty(default) then a
     * discriminant field will be added to serialized object.
     * @return discriminant name
     */
    String value() default "";
}
