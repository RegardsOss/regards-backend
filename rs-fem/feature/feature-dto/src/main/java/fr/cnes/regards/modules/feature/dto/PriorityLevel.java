/**
 *
 */
package fr.cnes.regards.modules.feature.dto;

/**
 * @author kevin
 */
public enum PriorityLevel {

    /**
     * CAUTION : Order in which enum values are defined in this class also define the enum value in database.
     * With Enumerated.ORDINAL, jpa do not use enum value but enum definition order as index.
     */
    LOW(0), NORMAL(1), HIGH(2);

    private final int priority;

    PriorityLevel(int priority) {
        this.priority = priority;
    }

    public int getPriorityLevel() {
        return priority;
    }

}
