package fr.cnes.regards.modules.toponyms.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymMetadata;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;


/**
 * Test of {@link TemporaryToponymsCleanService}
 * @author Iliana Ghazali
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=toponyms_service_clean_it"})
@RegardsTransactional
public class TemporaryToponymsCleanServiceIT extends AbstractRegardsIT {


    @Autowired
    private ToponymsRepository toponymsRepository;

    @Autowired
    private TemporaryToponymsCleanService toponymCleanService;


    private int nbToponyms = 10;

    private int nbExpired = 2; //must be inferior to nbToponyms

    @Test
    public void testCleanUp() {
        initNotVisibleToponyms();
        toponymCleanService.clean();
        Assert.assertEquals(this.nbToponyms - this.nbExpired , toponymsRepository.findByVisible(false, PageRequest.of(0, 100)).getTotalElements());

    }

    private List<Toponym> initNotVisibleToponyms() {
        List<Toponym> notVisibleToponyms = new ArrayList<>();
        for (int i = 0; i < nbToponyms; i++) {
            ToponymMetadata toponymMetaData = new ToponymMetadata();
            if(i < nbExpired) {
                toponymMetaData.setExpirationDate(OffsetDateTime.now());
            } else {
                toponymMetaData.setExpirationDate(OffsetDateTime.now().plusDays(20));
            }
            String name = "ToponymTest " + i;
            notVisibleToponyms.add(new Toponym(name, name, name, null, null, null, false, toponymMetaData));
        }
        return this.toponymsRepository.saveAll(notVisibleToponyms);
    }
}
