/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json.test.domain;

/**
 * for testing purpose, class that will be stored as a jsonb field into a postgreSQL database
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class JsonbEntity {

    /**
     * name
     */
    private String name;

    /**
     * content
     */
    private String content;

    public JsonbEntity() {
        super();
    }

    public JsonbEntity(String pName, String pContent) {
        this();
        name = pName;
        content = pContent;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String pContent) {
        content = pContent;
    }

    @Override
    public String toString() {
        return "JsonbEntity{ name = \"" + name + "\", content = \"" + content + "\" }";
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof JsonbEntity) && ((JsonbEntity) pOther).name.equals(name)
                && ((JsonbEntity) pOther).content.equals(content);
    }

}
