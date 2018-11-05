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
package fr.cnes.regards.framework.test.integration;

import java.util.Map;

import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.payload.RequestBodySnippet;

/**
 * REGARDS customization of {@link RequestBodySnippet}.
 * @author Marc Sordi
 */
public class RegardsRequestBodySnippet extends RequestBodySnippet {

    private static final String REQUEST_BODY = "body";

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = super.createModel(operation);
        cleanRequestBody(model);
        return model;
    }

    /**
     * If request body not useful, remove it for good Mustache template rendering
     */
    private void cleanRequestBody(Map<String, Object> model) {

        if (model.containsKey(REQUEST_BODY)) {
            Object body = model.get(REQUEST_BODY);
            if (body == null) {
                model.remove(REQUEST_BODY);
            } else {
                if (body instanceof String) {
                    String stringBody = (String) body;
                    if (stringBody.isEmpty()) {
                        model.remove(REQUEST_BODY);
                    }
                }
            }
        }
    }
}
