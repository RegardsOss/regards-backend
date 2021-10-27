
package fr.cnes.regards.modules.dam.client.entities;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestClient(name = "rs-dam", contextId = "rs-dam.attachment.client")
public interface IAttachmentClient {

    String TYPE_MAPPING = "/entities/{urn}/files";

    String ATTACHMENT_MAPPING = "/{checksum}";

    @RequestMapping(method = RequestMethod.GET, value = TYPE_MAPPING + ATTACHMENT_MAPPING,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response getFile(@PathVariable(name = "urn") String urn,
                     @PathVariable(name = "checksum") String checksum,
                     @RequestParam(name = "origin", required = false) String origin,
                     @RequestParam(name = "isContentInline", required = false) Boolean isContentInline);

}
