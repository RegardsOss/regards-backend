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
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.Type;

import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * @author Marc Sordi
 */
@SuppressWarnings("serial")
public class JsonBinaryMutableMutabilityPlan extends MutableMutabilityPlan<Object> {

    /**
     * JAVA object type : may be simple class or parameterized type
     */
    private transient Type type;

    public JsonBinaryMutableMutabilityPlan() {
        super();
    }

    /* (non-Javadoc)
     * @see org.hibernate.type.descriptor.java.MutableMutabilityPlan#deepCopyNotNull(java.lang.Object)
     */
    @Override
    protected Object deepCopyNotNull(Object value) {
        Type currentType = type == null ? value.getClass() : type;
        return GsonUtil.clone(value, currentType);
    }

    public void setType(Type type) {
        this.type = type;
    }

}
