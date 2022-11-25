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

import fr.cnes.regards.framework.utils.eureka.model.EurekaGetResponseDto;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

/**
 * @author Thibaud Michaudel
 **/
public class EurekaResponseUnmarshallTest {

    @Test
    public void unmarshallTest() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(EurekaGetResponseDto.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        InputStream response = getClass().getClassLoader().getResourceAsStream("get-response.xml");
        EurekaGetResponseDto responseDto = (EurekaGetResponseDto) jaxbUnmarshaller.unmarshal(response);
        Assert.assertNotNull(responseDto);
    }
}
