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
package fr.cnes.regards.modules.model.dto.properties;

public abstract class AbstractRangeProperty<T> extends AbstractProperty<AbstractRangeProperty.RangePropertyValue<T>> {

    public static class RangePropertyValue<T> {

        /**
         * Lower bound value
         */
        T gte;

        /**
         * Upper bound value
         */
        T lte;

        public RangePropertyValue() {
            // Nothing to do : default constructor for serialisation
        }

        public RangePropertyValue(T gte, T lte) {
            this.gte = gte;
            this.lte = lte;
        }

        public T getGte() {
            return gte;
        }

        public void setGte(T gte) {
            this.gte = gte;
        }

        public T getLte() {
            return lte;
        }

        public void setLte(T lte) {
            this.lte = lte;
        }
    }
}
