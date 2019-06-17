package fr.cnes.regards.modules.order.service;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;

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
        return TemplateConfigUtil.readTemplate(ASIDE_ORDERS_NOTIFICATION_TEMPLATE_NAME, "template/aside-orders-notification-template.html");
    }

    @Bean
    public Template orderCreated() throws IOException {
        return TemplateConfigUtil.readTemplate(ORDER_CREATED_TEMPLATE_NAME, "template/order-created-template.html");
    }

}
