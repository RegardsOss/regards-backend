package fr.cnes.regards.framework.s3.exception;

public class S3ClientException extends RuntimeException {

    private final int statusCode;

    public S3ClientException(String message) {
        super(message);
        statusCode = 500;
    }

    public S3ClientException(Throwable throwable) {
        super(throwable);
        statusCode = 500;
    }

    public S3ClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean is4XX() {
        return statusCode >= 400 && statusCode < 500;
    }
}
