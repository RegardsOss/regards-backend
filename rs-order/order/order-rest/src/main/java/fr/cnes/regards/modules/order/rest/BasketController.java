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
package fr.cnes.regards.modules.order.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.service.IBasketService;

/**
 * REST module controller
 *
 * TODO Description
 * @author TODO
 */
@RestController
@ModuleInfo(name = "order-rest", version = "2.0.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/order/basket")
public class BasketController implements IResourceController<Basket> {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IBasketService basketService;

    @ResourceAccess(description = "Add a selection to the basket")
    @RequestMapping(method = RequestMethod.POST, value = "/addSelection")
    public ResponseEntity<Resource<Basket>> addSelection(@RequestBody BasketSelectionRequest basketSelectionRequest) {
        String user = SecurityUtils.getActualUser();
        // Does a basket exist ?
        Basket basket = basketService.find(user);
        if (basket == null) {
            basket = basketService.create(user);
        }
        String openSearchRequest = basketSelectionRequest.computeOpenSearchRequest();
        return ResponseEntity.ok(toResource(basketService.addSelection(basket.getId(), openSearchRequest)));
    }

    @Override
    public Resource<Basket> toResource(Basket basket, Object... extras) {
        return resourceService.toResource(basket);
    }
}
