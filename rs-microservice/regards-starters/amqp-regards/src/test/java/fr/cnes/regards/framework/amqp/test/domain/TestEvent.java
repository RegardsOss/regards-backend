/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.test.domain;

/**
 * @author svissier
 *
 */
public class TestEvent {

    /**
     * content sent
     */
    private String content;

    public TestEvent() {

    }

    public TestEvent(final String pContent) {
        setContent(pContent);
    }

    public final String getContent() {
        return content;
    }

    public final void setContent(String pContent) {
        content = pContent;
    }

    @Override
    public String toString() {
        return "{\"content\" : " + content + "}";
    }

    @Override
    public boolean equals(Object pO) {
        return (pO instanceof TestEvent) && ((TestEvent) pO).content.equals(content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
