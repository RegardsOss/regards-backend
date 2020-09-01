package fr.cnes.regards.modules.processing.domain.factory;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

@Component
public class PUserAuthFactory implements IPUserAuthFactory {

    private final FeignSecurityManager feignSecurityManager;

    @Autowired
    public PUserAuthFactory(FeignSecurityManager feignSecurityManager) {
        this.feignSecurityManager = feignSecurityManager;
    }

    @Override public PUserAuth authFromUserEmailAndRole(String tenant, String email, String role) {
        try {
            FeignSecurityManager.asUser(email, role);
            String jwtToken = feignSecurityManager.getToken();
            return new PUserAuth(tenant, email, role, jwtToken);
        }
        finally {
            FeignSecurityManager.reset();
        }
    }

    @Override public PUserAuth authFromBatch(PBatch batch) {
        return authFromUserEmailAndRole(batch.getTenant(), batch.getUser(), batch.getUserRole());
    }

    @Override public PUserAuth fromContext(SecurityContext ctx) {
        JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
        UserDetails user = authentication.getUser();
        return new PUserAuth(authentication.getTenant(), user.getEmail(), user.getRole(), authentication.getJwt());
    }
}
