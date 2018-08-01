/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
