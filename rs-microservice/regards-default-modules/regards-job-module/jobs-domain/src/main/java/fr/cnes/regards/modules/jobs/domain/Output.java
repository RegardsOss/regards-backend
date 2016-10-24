/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.net.URI;

/**
 * Job output
 */
public class Output {

    /**
     * Job mimetype
     */
    private char mimeType;

    /**
     * Job path
     */
    private URI data;

    public char getMimeType() {
        return mimeType;
    }

    public void setMimeType(final char pMimeType) {
        mimeType = pMimeType;
    }

    public URI getData() {
        return data;
    }

    public void setData(final URI pData) {
        data = pData;
    }
}
