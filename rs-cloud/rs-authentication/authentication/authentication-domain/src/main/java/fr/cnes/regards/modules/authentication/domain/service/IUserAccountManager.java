package fr.cnes.regards.modules.authentication.domain.service;

import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import io.vavr.Tuple2;
import io.vavr.control.Try;

public interface IUserAccountManager {
    Try<String> createUserWithAccountAndGroups(String serviceProviderName, ServiceProviderAuthenticationInfo.UserInfo userInfo);
}
