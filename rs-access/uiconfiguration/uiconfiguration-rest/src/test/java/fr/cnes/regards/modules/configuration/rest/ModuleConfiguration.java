package fr.cnes.regards.modules.configuration.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineJsonClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Configuration
public class ModuleConfiguration {

    @Bean
    public ILegacySearchEngineJsonClient projectClient() {
        ILegacySearchEngineJsonClient mock = Mockito.mock(ILegacySearchEngineJsonClient.class);
        JsonObject stub = new JsonObject();
        JsonArray datasetList = new JsonArray();

        JsonObject firstDataset = new JsonObject();
        firstDataset.addProperty("ipId", "URN:A:B:C:D:E:F");
        JsonObject firstDatasetWithLinks = new JsonObject();
        firstDatasetWithLinks.add("content", firstDataset);
        datasetList.add(firstDatasetWithLinks);
        stub.add("content", datasetList);
        MultiValueMap attr = new LinkedMultiValueMap();

        Mockito.when(mock.searchDatasets(attr)).thenReturn(new ResponseEntity(stub, HttpStatus.OK));

        return mock;
    }

}
