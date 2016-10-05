package fr.cnes.regards.modules.project.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.project.fallback.ProjectsFallback;
import fr.cnes.regards.modules.project.signature.ProjectsSignature;

@FeignClient(name = "rs-admin", fallback = ProjectsFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface ProjectsClient extends ProjectsSignature {

}
