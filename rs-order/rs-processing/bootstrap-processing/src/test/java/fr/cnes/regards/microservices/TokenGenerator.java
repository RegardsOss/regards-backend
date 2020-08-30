package fr.cnes.regards.microservices;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import io.vavr.collection.HashMap;
import org.junit.Test;

import java.time.OffsetDateTime;

public class TokenGenerator {

    @Test
    public void make_token() {
        String token = new JWTService().generateToken(
            "PROJECTA",
            "user",
            "a@a.a",
            "EXPLOIT",
            OffsetDateTime.now().plusYears(10),
            HashMap.<String,Object>empty().toJavaMap(),
            "!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!",
            false
        );
        System.out.println(token);
    }

}
