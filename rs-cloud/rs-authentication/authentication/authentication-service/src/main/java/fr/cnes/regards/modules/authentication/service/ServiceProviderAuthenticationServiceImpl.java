package fr.cnes.regards.modules.authentication.service;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.domain.exception.serviceprovider.ServiceProviderPluginIllegalParameterException;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderAuthenticationService;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@MultitenantTransactional
public class ServiceProviderAuthenticationServiceImpl implements IServiceProviderAuthenticationService {

    private IServiceProviderRepository repository;

    private IPluginService pluginService;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private IUserAccountManager userAccountManager;

    private JWTService jwtService;

    public ServiceProviderAuthenticationServiceImpl() {}

    @Autowired
    public ServiceProviderAuthenticationServiceImpl(
        IServiceProviderRepository repository,
        IPluginService pluginService,
        IRuntimeTenantResolver runtimeTenantResolver,
        IUserAccountManager userAccountManager,
        JWTService jwtService
    ) {
        this.repository = repository;
        this.pluginService = pluginService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.userAccountManager = userAccountManager;
        this.jwtService = jwtService;
    }

    @Override
    public Try<String> authenticate(String serviceProviderName, ServiceProviderAuthenticationParams params) {
        return getPlugin(serviceProviderName)
            .flatMap(plugin -> {
                // HEMINGWAY limit work for frontend, use serviceProvider serviceProviderName to get plugin id here instead of in service ? or create custom exception in order to raison from inside service
                // Meanwhile, this should work
                if (plugin.getAuthenticationParamsType() != params.getClass()) {
                    return Try.failure(new ServiceProviderPluginIllegalParameterException("Invalid parameter type for service provider."));
                }
                return Try.success(plugin);
            })
            .flatMap(plugin -> plugin.authenticate(params))
            .flatMap(authInfo -> userAccountManager.createUserWithAccountAndGroups(serviceProviderName, authInfo.getUserInfo())
                .map(t -> t.map1(ignored -> authInfo)))
            .map(t -> {
                ServiceProviderAuthenticationInfo.UserInfo userInfo = t._1.getUserInfo();
                Map<String, String> authInfo = t._1.getAuthenticationInfo();
                Map<String, String> additionalClaims =
                    userInfo.getMetadata()
                        .merge(authInfo);

                String roleName = t._2;
                return jwtService.generateToken(
                    runtimeTenantResolver.getTenant(),
                    userInfo.getEmail(),
                    userInfo.getEmail(),
                    roleName,
                    new java.util.HashMap<>(additionalClaims.toJavaMap())
                );
            });
    }

    @Override
    public Try<Unit> deauthenticate(String serviceProviderName) {
        //TODO get jwt Token, decrypt and pass along
        JWTAuthentication jwt = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> jwtClaims =
            Option.of(jwt)
                .flatMap(auth -> Option.of(auth.getAdditionalParams()))
                .orElse(() -> Option.some(new java.util.HashMap<>()))
                .map(HashMap::ofAll)
                .get();

        return getPlugin(serviceProviderName)
            .flatMap(plugin -> plugin.deauthenticate(jwtClaims));
    }

    @VisibleForTesting
    protected Try<IServiceProviderPlugin<ServiceProviderAuthenticationParams, ServiceProviderAuthenticationInfo.AuthenticationInfo>> getPlugin(String serviceProviderName) {
        //noinspection unchecked
        return repository.findByName(serviceProviderName).toTry()
            .mapTry(sp -> pluginService.getPlugin(sp.getConfiguration().getBusinessId()));
    }
}
