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
package fr.cnes.regards.modules.acquisition.tools.compression.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * 
 * Cette classe designe l'exception que peut lever le paquetage de Compression. Elle doit etre traitee par le module ou
 * l'application appelant.
 * 
 * @author CS
 */
public class CompressionException extends ModuleException {

    /**
     * 
     */
    private static final long serialVersionUID = 7087726795253257854L;

    /**
     * Constructeur de copie.
     * 
     * @param pException
     *            L'exception a copier.
     * @since 1.0
     */
    public CompressionException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur de copie permettant d'ajouter un message.
     * 
     * @param pMessage
     *            Le message a ajouter
     * @param pException
     *            L'exception a copier.
     * @since 1.0
     */
    public CompressionException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur.
     * 
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     * @since 1.0
     */
    public CompressionException(String pMessage) {
        super(pMessage);
    }

}
