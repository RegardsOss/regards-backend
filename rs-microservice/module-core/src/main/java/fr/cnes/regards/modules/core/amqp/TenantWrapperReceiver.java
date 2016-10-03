/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/**
 * @author svissier
 *
 */
public class TenantWrapperReceiver {

    @Autowired
    private JWTService jwtService_;

    private final Handler handler_;

    /**
     * @param pHandler
     */
    public TenantWrapperReceiver(Handler<?> pHandler) {
        handler_ = pHandler;
    }

    public void dewrap(TenantWrapper pWrappedMessage) throws InvalidJwtException, MissingClaimException {
        String jwt = jwtService_.generateToken(pWrappedMessage.getTenant(), "", "", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(jwtService_.parseToken(new JWTAuthentication(jwt)));
        handler_.handle(pWrappedMessage.getContent());
    }

}
