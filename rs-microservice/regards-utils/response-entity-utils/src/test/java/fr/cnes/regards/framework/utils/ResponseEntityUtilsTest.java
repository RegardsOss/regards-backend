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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Thomas GUILLOU
 **/
public class ResponseEntityUtilsTest {

    static class ObjectInside {

        public String string = "string";
    }

    @Test
    public void test_extractBodyOrThrowSuccess() {
        ObjectInside objectInside = new ObjectInside();
        ResponseEntity<ObjectInside> responseEntity = new ResponseEntity<>(objectInside, HttpStatus.ACCEPTED);
        try {
            ObjectInside body = ResponseEntityUtils.extractBodyOrThrow(responseEntity, "");
            Assertions.assertEquals(body.string, "string");
        } catch (ModuleException e) {
            Assertions.fail();
        }
    }

    @Test
    public void test_extractContentOrThrowSuccess() {
        ObjectInside objectInside = new ObjectInside();
        EntityModel entityModel = EntityModel.of(objectInside);
        ResponseEntity<EntityModel<ObjectInside>> responseEntity = new ResponseEntity<>(entityModel,
                                                                                        HttpStatus.ACCEPTED);
        try {
            ObjectInside body = ResponseEntityUtils.extractContentOrThrow(responseEntity,
                                                                          () -> new RuntimeException("mon exception"));
            Assertions.assertEquals(body.string, "string");
        } catch (RuntimeException e) {
            Assertions.fail();
        }
    }

    @Test
    public void test_extractBodyOrThrowFail() {
        String exceptionMsg = "mon exception";
        ResponseEntity<ObjectInside> responseEntity = new ResponseEntity<>(null, HttpStatus.ACCEPTED);
        try {
            ResponseEntityUtils.extractBodyOrThrow(responseEntity, exceptionMsg);
            Assertions.fail();
        } catch (ModuleException e) {
            Assertions.assertEquals(exceptionMsg, e.getMessage());
        }
    }

    @Test
    public void test_extractContentOrThrowFail() {
        String exceptionMsg = "mon exception";
        ResponseEntity<EntityModel<ObjectInside>> responseEntity = new ResponseEntity<>(null, HttpStatus.ACCEPTED);
        try {
            ResponseEntityUtils.extractContentOrThrow(responseEntity, () -> new RuntimeException(exceptionMsg));
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertEquals(exceptionMsg, e.getMessage());
        }
    }
}