package fr.cnes.regards.modules.catalog.services.plugins;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IEntitiesServicePlugin;
import fr.cnes.regards.modules.catalog.services.plugins.CatalogPluginResponseFactory.CatalogPluginResponseType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;

@Plugin(description = "Plugin to allow download on multiple data selection by creating an archive.",
        id = "MultiDownloadPlugin", version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class MultiDownloadPlugin implements IEntitiesServicePlugin {

    public static final int MAX_NUMBER_OF_RESULTS = 1000;

    @Autowired
    private ISearchService searchService;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntities(List<String> pEntitiesId,
            HttpServletResponse response) {

        Set<DataFile> toDownloadFiles = Sets.newHashSet();
        List<String> uris = new ArrayList<>();
        List<String> other = new ArrayList<>();
        if ((pEntitiesId != null) && !pEntitiesId.isEmpty()) {
            Page<DataObject> results = getDataobjects(pEntitiesId, 0);
            if (results.getTotalElements() > MAX_NUMBER_OF_RESULTS) {
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
            }
            results.forEach(dataObject -> {
                dataObject.getFiles().forEach((type, file) -> {
                    if (DataType.RAWDATA.equals(type) && file.getOnline() && (file.getUri() != null)) {
                        // Add file to zipInputStream
                        toDownloadFiles.add(file);
                        uris.add(file.getUri().toString());
                    } else {
                        other.add(file.getName());
                    }
                });
            });
        }

        //        return CatalogPluginResponseFactory.createStreamSuccessResponse(response, getFilesAsZip(toDownloadFiles),
        //                                                                        "download.zip");
        return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                  new DownloadResponse(uris, other));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        // TODO Auto-generated method stub
        return null;
    }

    private Page<DataObject> getDataobjects(List<String> entityIds, int pageIndex) {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA);
        ICriterion ipIdsCrit = ICriterion.in("ipId", entityIds.toArray(new String[entityIds.size()]));
        PageRequest pageReq = new PageRequest(pageIndex, MAX_NUMBER_OF_RESULTS);
        return searchService.search(searchKey, pageReq, ipIdsCrit);
    }

    private StreamingResponseBody getFilesAsZip(Set<DataFile> files) {
        return (StreamingResponseBody) outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (DataFile file : files) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    ByteStreams.copy(DownloadUtils.getInputStream(new URL(file.getUri().toString())), zos);
                    zos.closeEntry();
                }
            }
        };
    }

}
