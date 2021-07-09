/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.rest.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.assertj.core.util.Lists;
import org.eclipse.jetty.http.HttpHeader;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import feign.Request;
import feign.Response;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;

@Component
@Primary
public class StorageClientMock implements IStorageRestClient {

    public static final String NO_QUOTA_MSG_STUB = "No quota to download this file";

    public static String TEST_FILE_CHECKSUM = "checusm_test_storage_client_mock";

    public static MediaType TEST_MEDIA_TYPE = MediaType.TEXT_PLAIN;

    @Override
    public Response downloadFile(String checksum, boolean isContentInline) {

        Map<String, Collection<String>> map = new HashMap<>();
        Request request = Request.create(Request.HttpMethod.GET, "test", map, Request.Body.empty());
        if (TEST_FILE_CHECKSUM.equals(checksum)) {
            try {
                File testFile = new File("src/test/resources/files/file1.txt");
                InputStream stream = new FileInputStream(testFile);
                Map<String, Collection<String>> headers = Maps.newHashMap();
                headers.put(HttpHeader.CONTENT_TYPE.toString(), Lists.newArrayList(TEST_MEDIA_TYPE.toString()));
                return Response.builder().status(HttpStatus.OK.value()).body(stream, (int) testFile.length())
                        .headers(headers).request(request).build();
            } catch (IOException e) {
                return Response.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .body(e.getMessage(), StandardCharsets.UTF_8).request(request).build();
            }
        } else {
            return Response.builder().status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .body(NO_QUOTA_MSG_STUB, StandardCharsets.UTF_8).request(request).build();
        }
    }

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve() {
        return null;
    }

    @Override
    public Response export() {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail,
            @Valid DownloadQuotaLimitsDto quotaLimits) {
        return null;
    }

    @Override
    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(String[] userEmails) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(String[] userEmails) {
        return null;
    }


}
