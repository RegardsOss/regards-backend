/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.utils.parser.rule;

import java.util.Arrays;

/**
 * An AbstractPropertyRule is the common ancestor class to rules that match properties by value.
 *
 * @author Julien Canches
 */
public abstract class AbstractPropertyRule extends AbstractRule {

    protected final String[] propertyPath;

    public AbstractPropertyRule(String property) {
        this.propertyPath = property.split("\\.");
    }

    public AbstractPropertyRule(String[] propertyPath) {
        this.propertyPath = propertyPath;
    }

    public String[] getPropertyPath() {
        return propertyPath;
    }

    @Override
    protected void toString(StringBuilder sb, boolean parenthesizeIfNeeded) {
        sb.append(String.join(".", propertyPath)).append(':');
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractPropertyRule that = (AbstractPropertyRule) o;
        return Arrays.equals(propertyPath, that.propertyPath);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(propertyPath);
    }
}
