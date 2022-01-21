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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration used to defined templates
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class OrderTemplateConf {

    public static final String ASIDE_ORDERS_NOTIFICATION_TEMPLATE_NAME = "ASIDE_ORDERS_NOTIFICATION_TEMPLATE";

    public static final String ORDER_CREATED_TEMPLATE_NAME = "ORDER_CREATED_TEMPLATE";

    @Bean
    public Template asideOrdersNotification() throws IOException {
        return TemplateConfigUtil.readTemplate(ASIDE_ORDERS_NOTIFICATION_TEMPLATE_NAME,
                                               "template/aside-orders-notification-template.html");
    }

    @Bean
    public Template orderCreated() throws IOException {
        return TemplateConfigUtil.readTemplate(ORDER_CREATED_TEMPLATE_NAME, "template/order-created-template.html");
    }

}
