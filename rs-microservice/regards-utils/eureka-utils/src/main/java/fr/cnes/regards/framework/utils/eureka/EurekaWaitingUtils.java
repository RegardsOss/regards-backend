/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.regards.framework.utils.eureka;

import fr.cnes.regards.framework.utils.eureka.model.EurekaApplication;
import fr.cnes.regards.framework.utils.eureka.model.EurekaGetResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Utility class used to block execution while waiting for Eureka Instances to be registered and running
 *
 * @author Thibaud Michaudel
 **/
public class EurekaWaitingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaWaitingUtils.class);

    private static final String UP_STATUS = "UP";

    public static final String EUREKA_APPS_RESOURCES = "/eureka/apps";

    /**
     * Block the thread until all defined microservices are registered in Eureka and running
     *
     * @param eurekaApiUrl         the url of the eureka api
     * @param servicesToWaitString the names of the microservices to wait as a string, the services names
     *                             are separated by commas without spaces
     * @param delayBeforeTries     the time in seconds before two status check
     */
    public static void waitForMicroservicesBeforeStart(String eurekaApiUrl,
                                                       String servicesToWaitString,
                                                       int delayBeforeTries) {
        List<String> servicesToWait = new ArrayList<>(Arrays.asList(servicesToWaitString.split(",")));
        //Remove the empty string that would appear when splitting an empty string list
        servicesToWait.remove("");
        LOGGER.info("Checking if the microservice can be started");
        while (!canBeStarted(eurekaApiUrl, servicesToWait)) {
            try {
                Thread.sleep(delayBeforeTries * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean canBeStarted(String eurekaApiUrl, List<String> servicesToWait) {
        try {
            List<EurekaApplication> services = getEurekaApps(eurekaApiUrl);
            List<String> missingServices = servicesToWait.stream()
                                                         .filter(serviceToWait -> !checkServiceStatus(services,
                                                                                                      serviceToWait))
                                                         .toList();
            if (missingServices.isEmpty()) {
                return true;
            }
            LOGGER.info("The microservice cannot be started because it is waiting for {}", missingServices);
        } catch (IOException | HttpResponseException e) {
            LOGGER.info("The microservice cannot be started because it is waiting for rs-registry");
        } catch (JAXBException e) {
            LOGGER.error("Error while unmarshalling response from rs-registry", e);
        }
        return false;
    }

    /**
     * Check if any instance of the given service is up
     *
     * @param services    the list of services
     * @param serviceName the service that should be up
     * @return true if any instance is up
     */
    private static boolean checkServiceStatus(List<EurekaApplication> services, String serviceName) {
        Optional<EurekaApplication> serviceToCheck = services.stream()
                                                             .filter(service -> service.getName()
                                                                                       .equalsIgnoreCase(serviceName))
                                                             .findFirst();
        return serviceToCheck.isPresent() && serviceToCheck.get()
                                                           .getInstances()
                                                           .stream()
                                                           .anyMatch(instance -> instance.getStatus()
                                                                                         .equals(UP_STATUS));
    }

    private static List<EurekaApplication> getEurekaApps(String eurekaApiUrl)
        throws IOException, JAXBException, HttpResponseException {
        URL url = new URL(eurekaApiUrl + EUREKA_APPS_RESOURCES);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            if (con.getResponseCode() == 200) {
                //rs-registry is up
                EurekaGetResponseDto responseDto = unmarshallResponse(con.getInputStream());
                return responseDto.getApplications();
            } else {
                throw new HttpResponseException(String.format("Error while getting Eureka instances, got error code %i",
                                                              con.getResponseCode()));
            }
        } finally {
            con.disconnect();
        }
    }

    private static EurekaGetResponseDto unmarshallResponse(InputStream responseStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(EurekaGetResponseDto.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return (EurekaGetResponseDto) jaxbUnmarshaller.unmarshal(responseStream);
    }
}
