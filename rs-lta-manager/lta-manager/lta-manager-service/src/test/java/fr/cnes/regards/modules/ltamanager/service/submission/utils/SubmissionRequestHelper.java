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
package fr.cnes.regards.modules.ltamanager.service.submission.utils;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionStatus;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmittedProduct;
import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * @author Thomas GUILLOU
 **/
@Service
public class SubmissionRequestHelper {

    private final ISubmissionRequestRepository requestRepository;

    private final LtaSettingService settingService;

    public SubmissionRequestHelper(ISubmissionRequestRepository requestRepository, LtaSettingService settingService) {
        this.requestRepository = requestRepository;
        this.settingService = settingService;
    }

    @NotNull
    public SubmissionRequest createAndSaveSubmissionRequest(SubmissionInfo info) {

        SubmissionStatus status = new SubmissionStatus(info.creationDate(),
                                                       info.statusDate(),
                                                       settingService.getRequestExpiresInHoursConfig(settingService.retrieve()),
                                                       info.state(),
                                                       "sample message");
        SubmittedProduct product = new SubmittedProduct(info.datatype(),
                                                        "model",
                                                        Paths.get("/path/example"),
                                                        new SubmissionRequestDto("test req n°" + UUID.randomUUID(),
                                                                                 UUID.randomUUID().toString(),
                                                                                 EntityType.DATA.toString(),
                                                                                 List.of(new ProductFileDto(LtaDataType.RAWDATA,
                                                                                                            "http://localhost/notexisting",
                                                                                                            "example.raw",
                                                                                                            "f016852239a8a919f05f6d2225c5aaca",
                                                                                                            MediaType.APPLICATION_OCTET_STREAM))));

        return requestRepository.save(new SubmissionRequest(product.getProduct().getCorrelationId(),
                                                            info.owner(),
                                                            info.session(),
                                                            false,
                                                            status,
                                                            product,
                                                            null,
                                                            null,
                                                            null));
    }

}

