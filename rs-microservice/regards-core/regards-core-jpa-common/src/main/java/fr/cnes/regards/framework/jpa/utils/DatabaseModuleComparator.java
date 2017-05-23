/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Comparator;

/**
 *
 * {@link DatabaseModule} comparator to sort module for migration priority
 *
 * @author Marc Sordi
 *
 */
public class DatabaseModuleComparator implements Comparator<DatabaseModule> {

    @Override
    public int compare(DatabaseModule module, DatabaseModule other) {
        return module.getWeight() > other.getWeight() ? 1 : -1;
    }

}
