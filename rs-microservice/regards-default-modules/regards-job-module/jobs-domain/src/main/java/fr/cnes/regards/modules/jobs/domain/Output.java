/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.net.URI;

/**
 * Job output
 * 
 * @author Léo Mieulet
 */
public class Output {

    /**
     * Job mimetype
     */
    private String mimeType;

    /**
     * Job path
     */
    private URI data;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String pMimeType) {
        mimeType = pMimeType;
    }

    public URI getData() {
        return data;
    }

    public void setData(final URI pData) {
        data = pData;
    }
}
