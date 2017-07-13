/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.annotations.Type;

/**
 * Job result
 * @author Léo Mieulet
 * @author oroussel
 */
@Embeddable
public class JobResult {

    /**
     * Job mimetype
     */
    @Column(length = 80, nullable = false)
    private String mimeType;

    /**
     * Job path
     */
    @Column(nullable = false)
    @Type(type = "text")
    private String uri;

    /**
     * Default constructor
     */
    public JobResult() {
        super();
    }

    /**
     * Constructor with the attributes
     * @param mimeType the uri's MimeType
     * @param uri the uri's URI
     */
    public JobResult(String mimeType, URI uri) {
        super();
        this.mimeType = mimeType;
        this.uri = uri.toString();
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String pMimeType) {
        mimeType = pMimeType;
    }

    public URI getUri() {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    public void setUri(URI uri) {
        this.uri = uri.toString();
    }
}
