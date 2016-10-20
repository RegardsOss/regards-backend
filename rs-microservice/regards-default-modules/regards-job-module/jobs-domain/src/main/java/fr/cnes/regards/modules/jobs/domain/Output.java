/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.net.URI;

public class Output {

    private char mimeType;

    private URI data;

    public char getMimeType() {
        return mimeType;
    }

    public void setMimeType(char pMimeType) {
        mimeType = pMimeType;
    }

    public URI getData() {
        return data;
    }

    public void setData(URI pData) {
        data = pData;
    }
}
