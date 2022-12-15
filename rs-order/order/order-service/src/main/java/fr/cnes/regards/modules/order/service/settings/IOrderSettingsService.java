package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.order.domain.settings.UserOrderParameters;

public interface IOrderSettingsService {

    void init() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException;

    UserOrderParameters getUserOrderParameters();

    void setUserOrderParameters(UserOrderParameters userOrderParameters) throws EntityException;

    Integer getAppSubOrderDuration();

    void setAppSubOrderDuration(int appSubOrderDuration) throws EntityException;

    Integer getExpirationMaxDurationInHours();

    void setExpirationMaxDurationInHours(int expirationMaxDurationInHours) throws EntityException;

}
