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
package fr.cnes.regards.modules.order.rest;

import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.dto.dto.OrderDto;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.hateoas.EntityModel;
import org.springframework.test.context.ActiveProfiles;

import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class OrderControllerLinksIT extends OrderControllerIT {

    @Test
    public void testHateoasLinks_Running() throws URISyntaxException, InterruptedException {
        Long id = createOrderAsRunning().getId();
        checkAdminLinks(id, "download", "pause");
        checkUserLinks(id, "download", "pause");
    }

    @Test
    public void testHateoasLinks_Pending() throws URISyntaxException, InterruptedException {
        Long id = createOrderAsPending().getId();
        checkAdminLinks(id, "pause");
        checkUserLinks(id, "pause");
    }

    @Test
    public void testHateoasLinks_Paused() throws URISyntaxException, InterruptedException {
        Long id = createOrderAs(OrderStatus.PAUSED);
        checkAdminLinks(id, "download", "resume", "delete");
        checkUserLinks(id, "download", "resume", "delete");
    }

    @Test
    public void testHateoasLinks_Done() throws URISyntaxException, InterruptedException {
        Long id = createOrderAs(OrderStatus.DONE);
        checkAdminLinks(id, "restart", "delete", "download");
        checkUserLinks(id, "restart", "delete", "download");
    }

    @Test
    public void testHateoasLinks_DoneWithWarning() throws URISyntaxException, InterruptedException {
        Long id = createOrderAs(OrderStatus.DONE_WITH_WARNING);
        checkAdminLinks(id, "restart", "retry", "delete", "download");
        checkUserLinks(id, "restart", "retry", "delete", "download");
    }

    @Test
    public void testHateoasLinks_Failed() throws URISyntaxException, InterruptedException {
        Long id = createOrderAs(OrderStatus.FAILED);
        checkAdminLinks(id, "restart", "retry", "delete", "download");
        checkUserLinks(id, "restart", "retry", "delete", "download");
    }

    @Test
    public void testHateoasLinks_Deleted() throws URISyntaxException, InterruptedException {
        Long id = createOrderAs(OrderStatus.DELETED);
        checkAdminLinks(id, "remove");
        checkBasicLinks(getOrderDtoAsUser(id));
    }

    private void checkUserLinks(Long id, String... links) throws InterruptedException {
        checkLinks(getOrderDtoAsUser(id), links);
    }

    private void checkAdminLinks(Long id, String... links) throws InterruptedException {
        checkLinks(getOrderDtoAsAdmin(id), links);
    }

    private void checkLinks(EntityModel<OrderDto> entityModel, String... links) {
        checkBasicLinks(entityModel);
        // Check proper number of links
        Assertions.assertTrue(entityModel.getLinks().hasSize(2 + links.length));
        // Check additional links
        Arrays.stream(links).forEach(link -> Assertions.assertTrue(entityModel.getLink(link).isPresent()));

    }

    private void checkBasicLinks(EntityModel<OrderDto> entityModel) {
        // Check basic links (always there)
        Assertions.assertTrue(entityModel.getLink(LinkRels.SELF).isPresent());
        Assertions.assertTrue(entityModel.getLink(LinkRels.LIST).isPresent());
    }

    private EntityModel<OrderDto> getOrderDtoAsAdmin(Long orderId) throws InterruptedException {
        return getOrderDtoEntityModel(orderId, projectAdminToken);
    }

    private EntityModel<OrderDto> getOrderDtoAsUser(Long orderId) throws InterruptedException {
        return getOrderDtoEntityModel(orderId, projectUserToken);
    }
}
