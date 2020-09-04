package fr.cnes.regards.modules.processing.exceptions;

import org.springframework.http.HttpStatus;

public enum ProcessingExceptionType {

    WORKDIR_CREATION_ERROR,
    INTERNAL_DOWNLOAD_ERROR,
    EXTERNAL_DOWNLOAD_ERROR,
    STORE_OUTPUTFILE_ERROR,
    ;

    private final HttpStatus status;

    ProcessingExceptionType(HttpStatus status) {
        this.status = status;
    }

    ProcessingExceptionType() {
        this(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
