package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import org.springframework.security.core.context.SecurityContext;

public interface IPUserAuthService {

    PUserAuth authFromUserEmailAndRole(String tenant, String email, String role);

    PUserAuth authFromBatch(PBatch batch);

    PUserAuth fromContext(SecurityContext ctx);
}
