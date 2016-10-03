/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/**
 * @author svissier
 *
 */
@Component
public class TenantWrapperReceiver {

    @Autowired
    private JWTService jwtService_;

    /**
     *
     */
    public TenantWrapperReceiver() {
    }

    /**
     *
     * @param pWrappedMessage
     * @param pHandler
     * @throws InvalidJwtException
     * @throws MissingClaimException
     */
    public final void dewrap(TenantWrapper pWrappedMessage, Handler pHandler)
            throws InvalidJwtException, MissingClaimException {
        String jwt = jwtService_.generateToken(pWrappedMessage.getTenant(), "", "", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(jwtService_.parseToken(new JWTAuthentication(jwt)));
        pHandler.handle(pWrappedMessage.getContent());
    }

}
