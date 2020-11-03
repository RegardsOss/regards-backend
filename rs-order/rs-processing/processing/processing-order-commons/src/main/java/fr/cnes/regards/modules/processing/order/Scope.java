package fr.cnes.regards.modules.processing.order;

/**
 * Provides information on how many executions are needed.
 */
public enum Scope {

    /**
     * One execution per suborder, the execution has all the files
     * in the suborder as input files
     */
    // TODO: add suborder constraints (max files, max size, etc.),
    // for now left to the rs-order suborder config (this could go in the OrderProcessInfo)
    SUBORDER,

    /**
     * One execution per item, the execution has only the files
     * for the given item.
     */
    ITEM;

}
