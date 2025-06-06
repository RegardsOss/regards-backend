/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of flyway scripts (sql and java) that pertain to a module. For a single module,
 * flyway scripts should be applied ordered by their version. A module can "depend" on other modules,
 * meaning that "depended on" modules should be upgraded before the "depending" module.
 * <p>
 * A module can have .sql scripts and java migrations (classes that implement {@link RegardsJavaMigration}).
 *
 * @author Marc Sordi
 */
/*package*/ final class DatabaseModule {

    /**
     * Module name
     */
    private final String name;

    /**
     * Whether the module has .sql scripts (if not, it should have at least one item in {@link #javaMigrations}).
     */
    private boolean hasSqlScripts;

    /**
     * The java migrations of the module. The method {@link RegardsJavaMigration#getModuleName()} does return the same
     * value as {@link #getName() this.getName()}.
     */
    private final List<RegardsJavaMigration> javaMigrations = new ArrayList<>(0);

    /**
     * The modules that this module depends upon.
     */
    private final Set<DatabaseModule> dependencies = new HashSet<>();

    /**
     * This field allows to sort modules for migration launching. It must set using {@link #computeWeight()}.
     */
    private int weight = 0;

    public DatabaseModule(String name) {
        this.name = name;
    }

    /**
     * Compute module weight regarding its max depth in the dependency tree
     */
    public void computeWeight() {
        weight = getMaxDepth();
    }

    /**
     * Compute max depth in the dependency tree of this module
     *
     * @return max depth
     */
    public int getMaxDepth() {
        // if no dependency, return 0
        if (dependencies.isEmpty()) {
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

    public void setHasSqlScripts(boolean hasSqlScripts) {
        this.hasSqlScripts = hasSqlScripts;
    }

    public boolean hasSqlScripts() {
        return hasSqlScripts;
    }

    public List<RegardsJavaMigration> getJavaMigrations() {
        return javaMigrations;
    }

    /**
     * Add a java migration to this module. The method {@link RegardsJavaMigration#getModuleName()} must return the same
     * value as {@link #getName() this.getName()}.
     */
    public void addJavaMigration(RegardsJavaMigration migration) {
        javaMigrations.add(migration);
    }

    /**
     * Returns the modules that this module depends upon. The migration of these modules must occur before the
     * migration of this module.
     */
    public Set<DatabaseModule> getDependencies() {
        return dependencies;
    }

    /**
     * Adds a module as dependent. Migration of dependency must occur before the receiver's.
     */
    public void addDependency(DatabaseModule dependency) {
        dependencies.add(dependency);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof DatabaseModule other) {
            return this.name.equals(other.name);
        }
        return false;
    }

    public int getWeight() {
        return weight;
    }

}
