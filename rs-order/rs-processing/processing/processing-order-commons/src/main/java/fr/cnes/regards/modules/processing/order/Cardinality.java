package fr.cnes.regards.modules.processing.order;

/**
 * Tells how many outputs there are by execution.
 *
 * Any process used by rs-order must provide this piece of information
 * as part of the OrderProcessInfo.
 *
 * This allows to know in advance how many OrderDataFile to create so that
 * the order metalink can be generated as soon as the order is accepted.
 */
public enum Cardinality {

    /**
     * Each execution produces exactly one output file.
     */
    ONE_PER_EXECUTION,

    /**
     * Each execution produces exactly one file per feature.
     */
    ONE_PER_FEATURE,

    /**
     * Each execution produces exactly one output file per input file.
     */
    ONE_PER_INPUT_FILE;

}
