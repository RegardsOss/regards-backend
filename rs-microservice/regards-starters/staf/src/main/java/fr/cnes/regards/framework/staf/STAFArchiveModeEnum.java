/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

/**
 * File archiving modes.
 * @author Sébastien Binda
 */
public enum STAFArchiveModeEnum {

    /**
     * File is archive as it is provided.
     */
    NORMAL,

    /**
     * File is archive in multiple parts of the raw file.
     */
    CUT,

    /**
     * File is archive as it is provided but is a part of a cuted raw file.
     */
    CUT_PART,

    /**
     * File is archive in a TAR.
     */
    TAR

}
