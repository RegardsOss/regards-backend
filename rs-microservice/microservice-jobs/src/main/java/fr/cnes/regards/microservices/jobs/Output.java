package fr.cnes.regards.microservices.jobs;

import java.net.URI;

public class Output {

    private char MimeType;

    private URI data;

    public char getMimeType() {
        return MimeType;
    }

    public void setMimeType(char pMimeType) {
        MimeType = pMimeType;
    }

    public URI getData() {
        return data;
    }

    public void setData(URI pData) {
        data = pData;
    }
}
