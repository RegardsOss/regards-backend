package fr.cnes.regards.modules.authentication.service;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.exception.serviceprovider.ServiceProviderPluginIllegalParameterException;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderAuthenticationService;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@MultitenantTransactional
public class ServiceProviderAuthenticationServiceImpl implements IServiceProviderAuthenticationService {

    private final IServiceProviderRepository repository;

    private final IPluginService pluginService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IUserAccountManager userAccountManager;

    private final JWTService jwtService;

    public ServiceProviderAuthenticationServiceImpl(IServiceProviderRepository repository,
                                                    IPluginService pluginService,
                                                    IRuntimeTenantResolver runtimeTenantResolver,
                                                    IUserAccountManager userAccountManager,
                                                    JWTService jwtService) {
        this.repository = repository;
        this.pluginService = pluginService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.userAccountManager = userAccountManager;
        this.jwtService = jwtService;
    }

    @Override
    public Try<Authentication> authenticate(String serviceProviderName, ServiceProviderAuthenticationParams params) {
        return getPlugin(serviceProviderName).flatMap(plugin -> {
                                                 if (plugin.getAuthenticationParamsType() != params.getClass()) {
                                                     return Try.failure(new ServiceProviderPluginIllegalParameterException(
                                                         "Invalid parameter type for service provider."));
                                                 }
                                                 return Try.success(plugin);
                                             })
                                             .flatMap(plugin -> plugin.authenticate(params))
                                             .flatMap(authInfo -> regardsAuthentication(serviceProviderName, authInfo));
    }

    @Override
    public Try<Unit> deauthenticate(String serviceProviderName) {
        JWTAuthentication jwt = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> jwtClaims = Option.of(jwt)
                                              .flatMap(auth -> Option.of(auth.getAdditionalParams()))
                                              .orElse(() -> Option.some(new java.util.HashMap<>()))
                                              .map(HashMap::ofAll)
                                              .get();
        return getPlugin(serviceProviderName).flatMap(plugin -> plugin.deauthenticate(jwtClaims));
    }

    @Override
    public Try<Authentication> verifyAndAuthenticate(String externalToken) {
        AtomicReference<String> currentServiceProviderName = new AtomicReference<>();
        return repository.findAll()
                         .toStream()
                         .map(ServiceProvider::getName)
                         .peek(currentServiceProviderName::set)
                         .map(this::getPlugin)
                         .map(t -> t.flatMap(plugin -> plugin.verify(externalToken))
                                    .flatMap(authInfo -> regardsAuthentication(currentServiceProviderName.get(),
                                                                               authInfo)))
                         .find(Try::isSuccess)
                         .map(Try::get)
                         .toTry(() -> new InsufficientAuthenticationException(
                             "Unable to find a Service Provider to successfully verify the provided token."));
    }

    private Try<Authentication> regardsAuthentication(String serviceProviderName,
                                                      ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfo.AuthenticationInfo> pAuthInfo) {
        return userAccountManager.createUserWithAccountAndGroups(pAuthInfo.getUserInfo(), serviceProviderName)
                                 .map(role -> Tuple.of(pAuthInfo.getUserInfo(),
                                                       role,
                                                       pAuthInfo.getAuthenticationInfo()))
                                 .map(t -> {
                                     ServiceProviderAuthenticationInfo.UserInfo userInfo = t._1;
                                     String roleName = t._2;
                                     Map<String, String> authInfo = t._3;
                                     Map<String, String> additionalClaims = userInfo.getMetadata().merge(authInfo);
                                     String tenant = runtimeTenantResolver.getTenant();
                                     String email = userInfo.getEmail();
                                     OffsetDateTime expirationDate = jwtService.getExpirationDate(OffsetDateTime.now());
                                     String token = jwtService.generateToken(tenant,
                                                                             email,
                                                                             email,
                                                                             roleName,
                                                                             expirationDate,
                                                                             new java.util.HashMap<>(additionalClaims.toJavaMap()));
                                     return new Authentication(tenant,
                                                               email,
                                                               roleName,
                                                               serviceProviderName,
                                                               token,
                                                               expirationDate);
                                 });
    }

    @VisibleForTesting
    protected Try<IServiceProviderPlugin<ServiceProviderAuthenticationParams, ServiceProviderAuthenticationInfo.AuthenticationInfo>> getPlugin(
        String serviceProviderName) {
        //noinspection unchecked
        return repository.findByName(serviceProviderName)
                         .toTry()
                         .mapTry(sp -> pluginService.getPlugin(sp.getConfiguration().getBusinessId()));
    }
}
