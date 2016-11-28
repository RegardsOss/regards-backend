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

    /**
     * Default constructor
     */
    public Output() {
        super();
    }

    /**
     * Constructor with the attributes
     * 
     * @param pMimeType
     *            the data's MimeType
     * @param pData
     *            the data's URI
     */
    public Output(String pMimeType, URI pData) {
        super();
        this.mimeType = pMimeType;
        this.data = pData;
    }

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
