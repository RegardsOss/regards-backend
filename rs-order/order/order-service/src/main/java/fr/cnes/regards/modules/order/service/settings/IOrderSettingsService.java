package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.order.domain.settings.UserOrderParameters;

public interface IOrderSettingsService {

    UserOrderParameters getUserOrderParameters();

    void setUserOrderParameters(UserOrderParameters userOrderParameters) throws EntityException;

    Integer getAppSubOrderDuration();

    void setAppSubOrderDuration(int appSubOrderDuration) throws EntityException;

}
