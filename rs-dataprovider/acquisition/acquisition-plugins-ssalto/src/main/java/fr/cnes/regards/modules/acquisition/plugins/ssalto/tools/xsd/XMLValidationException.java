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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.xsd;

/**
 * La classe <code>XMLValidationException</code> est levee quand une exception de validation est rencontree.
 * 
 * @author Christophe Mertz
 */
public class XMLValidationException extends Exception {

    private static final long serialVersionUID = -6936886653705675407L;

    public XMLValidationException() {
        super();
    }

    public XMLValidationException(Throwable pException) {
        super(pException);
    }

    public XMLValidationException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    public XMLValidationException(String pMessage) {
        super(pMessage);
    }

}
