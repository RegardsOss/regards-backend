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
package fr.cnes.regards.framework.utils.cycle.detection;

import java.util.HashSet;
import java.util.Set;

/**
 * Test class to test complex class plugin parameter type.
 * @author sbinda
 */
public class TestPojoWithSet {

    private Set<TestPojoChildWithSet> childs;

    public Set<TestPojoChildWithSet> getChilds() {
        return childs;
    }

    public void setChilds(Set<TestPojoChildWithSet> childs) {
        this.childs = childs;
    }

    public void addChild(TestPojoChildWithSet aChild) {
        if (this.childs == null) {
            this.childs = new HashSet<>();
        }
        this.childs.add(aChild);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((childs == null) ? 0 : childs.hashCode());
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
        TestPojoWithSet other = (TestPojoWithSet) obj;
        if (childs == null) {
            return other.childs == null;
        } else
            return childs.equals(other.childs);
    }

}