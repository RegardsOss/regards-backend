/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.exception;

import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception to indicate that the processing of a batch of AMQP messages has failed and cannot be retried.
 *
 * @author Iliana Ghazali
 **/
public class UnprocessableBatchException extends Exception {

    public UnprocessableBatchException(String message) {
        super(message);
    }

    public UnprocessableBatchException(String targetMethodName, Object[] arguments, Throwable cause) {
        super("Fail to invoke target method '" + targetMethodName + "' with argument type = [" + getArgumentTypes(
            arguments) + ", value = [" + ObjectUtils.nullSafeToString(arguments) + "]", cause);
    }

    private static List<String> getArgumentTypes(Object[] arguments) {
        List<String> argumentTypes = new ArrayList<>();
        for (Object argument : arguments) {
            argumentTypes.add(argument.getClass().toString());
        }
        return argumentTypes;
    }
}
