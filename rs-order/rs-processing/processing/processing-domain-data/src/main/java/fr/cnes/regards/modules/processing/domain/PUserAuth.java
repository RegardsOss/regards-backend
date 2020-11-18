package fr.cnes.regards.modules.processing.domain;

import lombok.Value;
import org.springframework.security.core.Authentication;

@Value
public class PUserAuth {

    String tenant;
    String email;
    String role;
    String authToken;

}
