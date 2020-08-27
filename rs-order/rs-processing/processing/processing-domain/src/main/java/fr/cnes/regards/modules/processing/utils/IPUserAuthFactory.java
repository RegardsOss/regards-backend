package fr.cnes.regards.modules.processing.utils;

import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.PBatch;
import org.springframework.security.core.context.SecurityContext;

public interface IPUserAuthFactory {

    PUserAuth authFromUserEmailAndRole(String tenant, String email, String role);

    PUserAuth authFromBatch(PBatch batch);

    PUserAuth fromContext(SecurityContext ctx);
}
