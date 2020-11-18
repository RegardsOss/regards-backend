package fr.cnes.regards.modules.processing.domain.exception;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;

public class ProcessingOutputFileException extends ProcessingException {

    protected final POutputFile outFile;

    public ProcessingOutputFileException(
            ProcessingExceptionType type,
            POutputFile outFile,
            String message
    ) {
        super(type, message);
        this.outFile = outFile;
    }

    public ProcessingOutputFileException(
            ProcessingExceptionType type,
            POutputFile outFile,
            String message,
            Throwable throwable
    ) {
        super(type, message, throwable);
        this.outFile = outFile;
    }

    public POutputFile getOutputFile() {
        return outFile;
    }


    @Override public final String getMessage() {
        return String.format("id=%s type=%s exec=%s outFile=%s message=%s",
             this.exceptionId, this.type, this.outFile.getExecId(), this.outFile.getId(), this.desc);
    }
}
