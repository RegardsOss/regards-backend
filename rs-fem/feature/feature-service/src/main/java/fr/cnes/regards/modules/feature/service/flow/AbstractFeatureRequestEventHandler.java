/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.flow;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import org.springframework.amqp.core.Message;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author Marc SORDI
 */
public abstract class AbstractFeatureRequestEventHandler<M> implements IBatchHandler<M> {

    private final Class<M> type;

    protected Validator validator;

    public AbstractFeatureRequestEventHandler(Class<M> type, Validator validator) {
        this.type = type;
        this.validator = validator;
    }

    @Override
    public Class<M> getMType() {
        return type;
    }

    @Override
    public boolean handleConversionError(Message message, String errorMessage) {
        return getFeatureService().denyMessage(message, errorMessage);
    }


    @Override
    public Errors validate(M message) {
        Errors errors = new BeanPropertyBindingResult(message, message.getClass().getName());
        validator.validate(message, errors);
        return errors;
    }

    public abstract IRequestDeniedService getFeatureService();
}
