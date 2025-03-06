package fr.cnes.regards.modules.fileaccess.dto.availability;

public enum NearlineFileStatusDtoStatus {
    /**
     * File is available for download
     **/
    AVAILABLE,
    /**
     * File is not available for download. A restore action must be processed first.
     **/
    UNAVAILABLE,
    /**
     * Error during availability status. File could be available or not.
     **/
    ERROR
}
