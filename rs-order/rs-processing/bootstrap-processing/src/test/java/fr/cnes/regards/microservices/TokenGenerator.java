package fr.cnes.regards.microservices;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import io.vavr.collection.HashMap;
import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    @Test
    public void datetimes() {
        OffsetDateTime y2k = OffsetDateTime.ofInstant(Instant.parse("2000-01-01T00:00:00.000Z"), ZoneId.of("UTC"));
        OffsetDateTime y2p1k = y2k.plusYears(100L);
        System.out.println(y2p1k.toEpochSecond());
        System.out.println(y2p1k);

    }

}
