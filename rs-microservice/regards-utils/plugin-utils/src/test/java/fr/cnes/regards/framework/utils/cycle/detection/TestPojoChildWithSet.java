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
package fr.cnes.regards.framework.utils.cycle.detection;

import java.util.HashSet;
import java.util.Set;

/**
 * Test class to test complex class plugin parameter type.
 * @author sbinda
 */
public class TestPojoChildWithSet {

    private Set<TestPojo> pojos;

    public Set<TestPojo> getPojos() {
        return pojos;
    }

    public void setPojos(Set<TestPojo> pojos) {
        this.pojos = pojos;
    }

    public void addPojo(TestPojo aPojo) {
        if (this.pojos == null) {
            this.pojos = new HashSet<TestPojo>();
        }
        this.pojos.add(aPojo);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pojos == null) ? 0 : pojos.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TestPojoChildWithSet other = (TestPojoChildWithSet) obj;
        if (pojos == null) {
            if (other.pojos != null) {
                return false;
            }
        } else if (!pojos.equals(other.pojos)) {
            return false;
        }
        return true;
    }

}