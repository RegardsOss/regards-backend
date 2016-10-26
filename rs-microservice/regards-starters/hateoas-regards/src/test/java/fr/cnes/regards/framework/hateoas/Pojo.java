/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

/**
 *
 * Sample POJO
 * 
 * @author msordi
 *
 */
public class Pojo {

    /**
     * Id
     */
    private final Long id;

    /**
     * Content
     */
    private final String content;

    public Pojo(Long pId, String pContent) {
        this.id = pId;
        this.content = pContent;

    }

    public String getContent() {
        return content;
    }

    public Long getId() {
        return id;
    }
}
