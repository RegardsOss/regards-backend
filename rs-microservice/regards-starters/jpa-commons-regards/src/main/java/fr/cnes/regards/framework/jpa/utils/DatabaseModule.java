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
package fr.cnes.regards.framework.jpa.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a database module
 * @author Marc Sordi
 */
public class DatabaseModule {

    /**
     * Module name
     */
    private String name;

    /**
     * Dependent module names
     */
    private Set<DatabaseModule> dependencies;

    /**
     * This field allows to sort modules for migration launching
     */
    private int weight = 0;

    public DatabaseModule(String name, DatabaseModule... dependencies) {
        this.name = name;
        this.dependencies = new HashSet<>(Arrays.asList(dependencies));
    }

    /**
     * Compute module weight regarding its max depth in the dependency tree
     */
    public void computeWeight() {
        weight = getMaxDepth();
    }

    /**
     * Compute max depth in the dependency tree of this module
     * @return max depth
     */
    public int getMaxDepth() {

        // if no dependency, return 0
        if ((dependencies == null) || dependencies.isEmpty()) {
            return 0;
        }

        // if dependencies, return 1 + max depth
        int depWeight = 0;
        for (DatabaseModule dep : dependencies) {
            depWeight = Math.max(depWeight, dep.getMaxDepth());
        }
        return 1 + depWeight;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public Set<DatabaseModule> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<DatabaseModule> pDependencies) {
        dependencies = pDependencies;
    }

    public void addDependency(DatabaseModule dependency) {
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }
        dependencies.add(dependency);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
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
        DatabaseModule other = (DatabaseModule) obj;
        if (dependencies == null) {
            if (other.dependencies != null) {
                return false;
            }
        } else if (!dependencies.equals(other.dependencies)) {
            return false;
        }
        if (name == null) {
            return other.name == null;
        } else
            return name.equals(other.name);
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int pWeight) {
        weight = pWeight;
    }
}
