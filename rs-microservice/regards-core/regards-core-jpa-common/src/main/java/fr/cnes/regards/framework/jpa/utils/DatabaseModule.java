/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * This class represents a database module
 * @author Marc Sordi
 *
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
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int pWeight) {
        weight = pWeight;
    }
}
