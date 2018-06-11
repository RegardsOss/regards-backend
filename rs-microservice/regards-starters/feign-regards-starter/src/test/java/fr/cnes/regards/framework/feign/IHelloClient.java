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
package fr.cnes.regards.framework.feign;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.FeignClientTests.Hello;
import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 * TODO
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "localapp", url = "http://localhost:30333")
public interface IHelloClient {

    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    ResponseEntity<Hello> getHello();

    @RequestMapping(method = RequestMethod.GET, value = "/hello404")
    ResponseEntity<Hello> getHello404();

    @RequestMapping(method = RequestMethod.GET, value = "/hello503")
    ResponseEntity<List<Hello>> getHello503();
}
