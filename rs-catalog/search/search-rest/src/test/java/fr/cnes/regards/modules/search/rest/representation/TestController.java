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
package fr.cnes.regards.modules.search.rest.representation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for the purpose of this test: check that is the return body is not an AbstractEntity then it doesn't
 * take into account the representation http message converter
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
public class TestController {

    public static final String TEST_BODY = "hello world, it seems that it works!";

    public static final String TEST_PATH = "/test";

    @RequestMapping(path = TEST_PATH)
    @ResponseBody
    public ResponseEntity<String> test() {
        return new ResponseEntity<>(TEST_BODY, HttpStatus.OK);
    }

}
