/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.sip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SearchSIPsParameters;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class SIPService implements ISIPService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SIPService.class);

    public static final String MD5_ALGORITHM = "MD5";

    @Autowired
    private Gson gson;

    @Autowired
    private ISIPRepository sipRepository;

    @Override
    public Page<SIPEntity> search(SearchSIPsParameters params, Pageable page) {
        return sipRepository.loadAll(SIPEntitySpecifications
                .search(params.getProviderIds(), null, params.getSessionOwner(), params.getSession(),
                        params.getIpType(), params.getFrom(), params.getStates(), true, params.getTags(),
                        params.getCategories(), page), page);
    }

    @Override
    public Optional<SIPEntity> getEntity(String sipId) {
        return sipRepository.findOneBySipId(sipId.toString());
    }

    @Override
    public void processDeletion(String sipId, boolean deleteIrrevocably) {
        Optional<SIPEntity> optionalSIPEntity = sipRepository.findOneBySipId(sipId);
        if (optionalSIPEntity.isPresent()) {
            SIPEntity sipEntity = optionalSIPEntity.get();
            if (!deleteIrrevocably) {
                // Mark the SIP correctly deleted
                sipEntity.setState(SIPState.DELETED);
                save(sipEntity);
            } else {
                sipRepository.delete(sipEntity);
            }
        }
    }

    @Override
    public boolean validatedVersionExists(String providerId) {
        return sipRepository.countByProviderId(providerId) > 0;
    }

    @Override
    public SIPEntity save(SIPEntity sip) {
        // update last update
        sip.setLastUpdate(OffsetDateTime.now());
        // save
        return sipRepository.save(sip);
    }

    @Override
    public String calculateChecksum(SIP sip) throws NoSuchAlgorithmException, IOException {
        String jsonSip = gson.toJson(sip);
        InputStream inputStream = new ByteArrayInputStream(jsonSip.getBytes());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public boolean isAlreadyIngested(String checksum) {
        return sipRepository.isAlreadyIngested(checksum);
    }

    @Override
    public Integer getNextVersion(SIP sip) {
        return sipRepository.getNextVersion(sip.getId());
    }
}
