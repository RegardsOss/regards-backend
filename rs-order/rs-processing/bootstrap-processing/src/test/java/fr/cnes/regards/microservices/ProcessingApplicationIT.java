package fr.cnes.regards.microservices;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProcessingApplication.class)
@ActiveProfiles({"default", "test"})
public class ProcessingApplicationIT {

    @Test
    public void test_launchable() {
        // Tests simply that launching the application works,
        // making sure we have a correct configuration.



    }

}