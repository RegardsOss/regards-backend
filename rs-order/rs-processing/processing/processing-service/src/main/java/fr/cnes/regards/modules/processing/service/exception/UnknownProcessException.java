package fr.cnes.regards.modules.processing.service.exception;

public class UnknownProcessException extends Exception {

    private final String processName;

    public UnknownProcessException(String processName) {
        this.processName = processName;
    }
}
