package fr.cnes.regards.modules.authentication.dao.entity.mapping;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.authentication.dao.entity.ServiceProviderEntity;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DomainEntityMapperTest {

    private final Random random = new Random();

    private final DomainEntityMapperImpl mapper = new DomainEntityMapperImpl();

    private static String randomStr() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void fromDomain() {
        LongStream.range(0, random.nextInt(150)+50)
            .forEach(i -> {
                ServiceProvider domain = new ServiceProvider(
                    randomStr(),
                    randomStr(),
                    new PluginConfiguration(
                        randomStr(),
                        randomStr()
                    )
                );
                assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
            });
    }

    @Test
    public void fromEntity() {
        LongStream.range(0, random.nextInt(150)+50)
            .forEach(i -> {
                ServiceProviderEntity entity = new ServiceProviderEntity(
                    randomStr(),
                    randomStr(),
                    new PluginConfiguration(
                        randomStr(),
                        randomStr()
                    )
                );
                assertEquals(entity, mapper.toEntity(mapper.toDomain(entity)));
            });
    }
}
