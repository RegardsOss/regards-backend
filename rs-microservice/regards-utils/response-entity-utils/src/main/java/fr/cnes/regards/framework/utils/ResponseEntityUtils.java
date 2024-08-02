/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * This class contains utilities to simply extract content of a {@link ResponseEntity}.
 * These utilities avoid multiple null checks, and potentials NullPointerException.
 *
 * @author Thomas GUILLOU
 **/
public final class ResponseEntityUtils {

    private ResponseEntityUtils() {
    }

    /**
     * Extract body of the ResponseEntity.
     * Throw a ModuleException if ResponseEntity is null or body extracted is null
     */
    public static <T> T extractBodyOrThrow(@Nullable ResponseEntity<T> response, String errorMsg)
        throws ModuleException {
        return extractBodyOrThrow(response, () -> new ModuleException(errorMsg));
    }

    /**
     * Extract content of the EntityModel of an ResponseEntity.
     * Throw a ModuleException if
     * <li>ResponseEntity is null</li>
     * <li>EntityModel inside ResponseEntity is null</li>
     * <li>content of EntityModel is null</li>
     */
    public static <T> T extractContentOrThrow(@Nullable ResponseEntity<EntityModel<T>> response, String errorMsg)
        throws ModuleException {
        return extractContentOrThrow(response, () -> new ModuleException(errorMsg));
    }

    /**
     * Extract body of the ResponseEntity.
     * Throw the exception indicated in parameter if ResponseEntity is null or body extracted is null
     */
    public static <T, X extends Throwable> T extractBodyOrThrow(@Nullable ResponseEntity<T> response,
                                                                Supplier<? extends X> exceptionSupplier) throws X {
        T body = extractBodyOrNull(response);
        if (body == null) {
            throw (X) exceptionSupplier.get();
        }
        return body;
    }

    /**
     * Extract content of the EntityModel of an ResponseEntity.
     * Throw the exception indicated in parameter if
     * <li>ResponseEntity is null</li>
     * <li>EntityModel inside ResponseEntity is null</li>
     * <li>content of EntityModel is null</li>
     */
    public static <T, X extends Throwable> T extractContentOrThrow(@Nullable ResponseEntity<EntityModel<T>> response,
                                                                   Supplier<? extends X> exceptionSupplier) throws X {
        T content = extractContentOrNull(response);
        if (content == null) {
            throw (X) exceptionSupplier.get();
        }
        return content;
    }

    /**
     * Extract content of the EntityModel of an ResponseEntity.
     */
    @Nullable
    public static <T> T extractContentOrNull(@Nullable ResponseEntity<EntityModel<T>> response) {
        EntityModel<T> body = extractBodyOrNull(response);
        if (body == null) {
            return null;
        }
        return body.getContent();
    }

    /**
     * Extract body of the ResponseEntity.
     */
    @Nullable
    public static <T> T extractBodyOrNull(@Nullable ResponseEntity<T> response) {
        if (response == null) {
            return null;
        }
        return response.getBody();
    }
}