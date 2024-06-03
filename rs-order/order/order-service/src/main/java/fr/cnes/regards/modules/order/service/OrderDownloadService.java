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
package fr.cnes.regards.modules.order.service;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.metalink.schema.*;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@MultitenantTransactional
@RefreshScope
public class OrderDownloadService implements IOrderDownloadService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDownloadService.class);

    private static final String METALINK_XML_SCHEMA_NAME = "metalink.xsd";

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{null}}")
    private String noProxyHostsString;

    private final Set<String> noProxyHosts = Sets.newHashSet();

    private Proxy proxy;

    private final IOrderRepository orderRepository;

    private final IOrderDataFileService dataFileService;

    private final IOrderJobService orderJobService;

    private final IAuthenticationResolver authResolver;

    private final OrderHelperService orderHelperService;

    private final IProjectsClient projectClient;

    private final IStorageRestClient storageClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProcessingEventSender processingEventSender;

    private final IAttachmentClient attachmentClient;

    public OrderDownloadService(IOrderRepository orderRepository,
                                IOrderDataFileService dataFileService,
                                IOrderJobService orderJobService,
                                IAuthenticationResolver authResolver,
                                OrderHelperService orderHelperService,
                                IProjectsClient projectClient,
                                IStorageRestClient storageClient,
                                IRuntimeTenantResolver runtimeTenantResolver,
                                IProcessingEventSender processingEventSender,
                                IAttachmentClient attachmentClient) {
        this.orderRepository = orderRepository;
        this.dataFileService = dataFileService;
        this.orderJobService = orderJobService;
        this.authResolver = authResolver;
        this.orderHelperService = orderHelperService;
        this.projectClient = projectClient;
        this.storageClient = storageClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.processingEventSender = processingEventSender;
        this.attachmentClient = attachmentClient;
    }

    @Override
    public void afterPropertiesSet() {
        proxy = Strings.isNullOrEmpty(proxyHost) ?
            Proxy.NO_PROXY :
            new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        if (noProxyHostsString != null) {
            Collections.addAll(noProxyHosts, noProxyHostsString.split("\\s*,\\s*"));
        }
    }

    @Override
    public void downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os) {
        List<OrderDataFile> availableFiles = new ArrayList<>(inDataFiles);
        List<OrderDataFile> downloadedFiles = new ArrayList<>();
        // A multiset to manage multi-occurrences of files with same name
        Multiset<String> fileNamesInZip = HashMultiset.create();
        List<Pair<OrderDataFile, String>> downloadErrorFiles = new ArrayList<>();

        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(os)) {
            zos.setEncoding("ASCII");
            zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
            for (Iterator<OrderDataFile> i = availableFiles.iterator(); i.hasNext(); ) {
                OrderDataFile dataFile = i.next();
                // Check if file is already download in zip
                if (!fileAlreadyInZip(dataFile, downloadedFiles)) {
                    // Download file
                    if (downloadOrderDataFile(dataFile, i, fileNamesInZip, downloadErrorFiles, zos)) {
                        downloadedFiles.add(dataFile);
                    }
                }
            }
            if (!downloadErrorFiles.isEmpty()) {
                zos.putArchiveEntry(new ZipArchiveEntry("NOTICE.txt"));
                StringJoiner joiner = new StringJoiner("\n");
                downloadErrorFiles.forEach(p -> joiner.add(String.format("Failed to download file (%s): %s.",
                                                                         p.getLeft().getFilename(),
                                                                         p.getRight())));
                zos.write(joiner.toString().getBytes());
                zos.closeArchiveEntry();
            }
            zos.flush();
            zos.finish();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot create ZIP file.", e);
        }
        // Set statuses of all downloaded files
        availableFiles.forEach(f -> f.setState(FileState.DOWNLOADED));
        // Set statuses of all not downloaded files
        downloadErrorFiles.forEach(f -> f.getLeft().setState(FileState.DOWNLOAD_ERROR));
        // use one set to save everybody
        availableFiles.addAll(downloadErrorFiles.stream().map(Pair::getLeft).toList());
        dataFileService.save(availableFiles);

        processingEventSender.sendDownloadedFilesNotification(availableFiles);

        // Don't forget to manage user order jobs (maybe order is in waitingForUser state)
        orderJobService.manageUserOrderStorageFilesJobInfos(orderOwner);
    }

    /**
     * Download given {@link OrderDataFile} from external system (file or http protocol) or throught regards storage
     * microservice into a {@link ZipArchiveOutputStream}.
     */
    private boolean downloadOrderDataFile(OrderDataFile dataFile,
                                          Iterator<OrderDataFile> currentFileIterator,
                                          Multiset<String> fileNamesInZip,
                                          List<Pair<OrderDataFile, String>> downloadErrorFiles,
                                          ZipArchiveOutputStream zos) throws IOException {
        boolean downloadOk;
        if (dataFile.isReference()) {
            // Externally downloadable
            dataFile.setDownloadError(null);
            return downloadExternalDataFileToZip(downloadErrorFiles,
                                                 zos,
                                                 fileNamesInZip,
                                                 currentFileIterator,
                                                 dataFile,
                                                 dataFile.getIpId().toString());
        } else {
            downloadOk = downloadDataFileToZip(dataFile, currentFileIterator, fileNamesInZip, downloadErrorFiles, zos);
        }
        return downloadOk;
    }

    /**
     * Download given {@link OrderDataFile} into result {@link ZipArchiveOutputStream}.
     * The given {@link OrderDataFile} can be stored in :
     * <ul>
     * <li>dam microservice, for dataset attached files</li>
     * <li>storage microservice, for features files</li>
     * </ul>
     */
    private boolean downloadDataFileToZip(OrderDataFile dataFile,
                                          Iterator<OrderDataFile> currentFileIterator,
                                          Multiset<String> fileNamesInZip,
                                          List<Pair<OrderDataFile, String>> downloadErrorFiles,
                                          ZipArchiveOutputStream zos) {
        String errorPrefix = "Error while downloading file.";
        String aip = dataFile.getIpId().toString();
        dataFile.setDownloadError(null);
        boolean downloadOk = false;

        Optional<Response> responseOpt = downloadDataFile(dataFile, errorPrefix, null);
        if (responseOpt.isPresent()) {
            Response response = responseOpt.get();
            if (response.status() != HttpStatus.OK.value()) {
                String adminErrorMessage = String.format("Cannot retrieve data file (aip : %s, checksum : %s). Feign "
                                                         + "downloadFile method returns %s",
                                                         aip,
                                                         dataFile.getChecksum(),
                                                         responseOpt.map(Response::toString).orElse("null"));
                handleDownloadError(downloadErrorFiles,
                                    currentFileIterator,
                                    dataFile,
                                    adminErrorMessage,
                                    humanizeError(responseOpt));
            } else { // Download ok
                Long contentLength = Long.parseLong(response.headers()
                                                            .get(OrderDataFileService.CONTENT_LENGTH_HEADER)
                                                            .iterator()
                                                            .next());
                try (InputStream is = response.body().asInputStream()) {
                    readInputStreamAndAddToZip(downloadErrorFiles,
                                               zos,
                                               fileNamesInZip,
                                               currentFileIterator,
                                               dataFile,
                                               Optional.of(contentLength),
                                               aip,
                                               is);
                    downloadOk = true;
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                    handleDownloadError(downloadErrorFiles,
                                        currentFileIterator,
                                        dataFile,
                                        String.format("Error while downloading internal file %s", dataFile.getUrl()),
                                        "Error during file download");
                }
            }
        }
        return downloadOk;
    }

    public Optional<Response> downloadDataFile(OrderDataFile dataFile, String errorPrefix, @Nullable String asUser) {
        Response response = null;
        try {
            // To download through storage client we must be authenticated as user in order to
            // impact the download quotas, but we upgrade the privileges so that the request passes.
            FeignSecurityManager.asUser(asUser != null ? asUser : authResolver.getUser(),
                                        DefaultRole.PROJECT_ADMIN.name());
            // To download file with accessrights checked, we should use catalogDownloadClient
            // but the accessRight have already been checked here.
            if (dataFile.urlIsFromDam()) {
                response = attachmentClient.getFile(dataFile.getIpId().toString(), dataFile.getChecksum(), null, false);
            } else {
                response = storageClient.downloadFile(dataFile.getChecksum(), false);
            }
        } catch (RuntimeException e) {
            String stack = getStack(e);
            String source = dataFile.urlIsFromDam() ? "Error from Dam download" : "Error from Storage download";
            LOGGER.error(errorPrefix + " " + source, e);
            dataFile.setDownloadError(String.format("%s %s\n%s", errorPrefix, source, stack));
        } finally {
            FeignSecurityManager.reset();
        }
        return Optional.ofNullable(response);
    }

    private boolean fileAlreadyInZip(OrderDataFile dataFile, List<OrderDataFile> downloadedFiles) {
        return downloadedFiles.stream().anyMatch(f -> {
            if (f.getChecksum() != null) {
                return f.getChecksum().equals(dataFile.getChecksum()) && f.getFilename().equals(dataFile.getFilename());
            } else {
                return f.getFilename().equals(dataFile.getFilename());
            }
        });
    }

    protected boolean downloadExternalDataFileToZip(List<Pair<OrderDataFile, String>> downloadErrorFiles,
                                                    ZipArchiveOutputStream zos,
                                                    Multiset<String> fileNamesInZip,
                                                    Iterator<OrderDataFile> currentFileIterator,
                                                    OrderDataFile dataFile,
                                                    String dataObjectIpId) {
        boolean downloadOk = false;
        try (InputStream is = DownloadUtils.getInputStreamThroughProxy(new URL(dataFile.getUrl()),
                                                                       proxy,
                                                                       noProxyHosts,
                                                                       10_000,
                                                                       Collections.emptyList())) {
            // FIXME : If file is external (http url not stored by storage, file size of the dataFile can be null.
            // In this case, if the real file size is over 4Gb The apache zip library is not available to use
            // the right implementation (64bits).
            readInputStreamAndAddToZip(downloadErrorFiles,
                                       zos,
                                       fileNamesInZip,
                                       currentFileIterator,
                                       dataFile,
                                       Optional.ofNullable(dataFile.getFilesize()),
                                       dataObjectIpId,
                                       is);
            downloadOk = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            handleDownloadError(downloadErrorFiles,
                                currentFileIterator,
                                dataFile,
                                String.format("Error while downloading external file %s", dataFile.getUrl()),
                                "Error during file download");
        }
        return downloadOk;
    }

    private void handleDownloadError(List<Pair<OrderDataFile, String>> downloadErrorFiles,
                                     Iterator<OrderDataFile> currentFileIterator,
                                     OrderDataFile dataFile,
                                     String adminResponseErrorMessage,
                                     String userResponseErrorMessage) {
        LOGGER.error(adminResponseErrorMessage);
        dataFile.setDownloadError(adminResponseErrorMessage);
        downloadErrorFiles.add(Pair.of(dataFile, userResponseErrorMessage));
        currentFileIterator.remove();
    }

    private String humanizeError(Optional<Response> response) {
        return response.map(r -> {
            Response.Body body = r.body();
            boolean nullBody = body == null;
            if (r.status() == 429) {
                if (nullBody) {
                    return "Download failed due to exceeded quota";
                }

                try (InputStream is = body.asInputStream()) {
                    return IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.debug("I/O error ready response body", e);
                    return "Download failed due to exceeded quota";
                }
            } else {
                return String.format("Server returned HTTP error code %d", r.status());
            }
        }).orElse("Server returned no content");
    }

    protected String getStack(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void readInputStreamAndAddToZip(List<Pair<OrderDataFile, String>> downloadErrorFiles,
                                            ZipArchiveOutputStream zos,
                                            Multiset<String> fileNamesInZip,
                                            Iterator<OrderDataFile> i,
                                            OrderDataFile dataFile,
                                            Optional<Long> realContentLength,
                                            String dataObjectIpId,
                                            InputStream is) throws IOException {
        // Add filename to multiset
        String filename = dataFile.getFilename();
        if (filename == null) {
            filename = dataFile.getUrl().substring(dataFile.getUrl().lastIndexOf('/') + 1);
        }
        fileNamesInZip.add(filename);
        // If same file appears several times, add "(n)" juste before extension.
        int filenameCount = fileNamesInZip.count(filename);
        if (filenameCount > 1) {
            String suffix = " (" + (filenameCount - 1) + ")";
            int lastDotIdx = filename.lastIndexOf('.');
            if (lastDotIdx != -1) {
                filename = filename.substring(0, lastDotIdx) + suffix + filename.substring(lastDotIdx);
            } else { // No extension
                filename += suffix;
            }
        }
        ZipArchiveEntry ze = new ZipArchiveEntry(filename);
        realContentLength.ifPresent(ze::setSize);
        zos.putArchiveEntry(ze);
        long copiedBytes = 0L;
        try {
            copiedBytes = ByteStreams.copy(is, zos);
        } finally {
            zos.closeArchiveEntry();
        }

        // We can only check copied bytes if we know expected size (ie if file is internal)
        Long fileSize = realContentLength.orElse(dataFile.getFilesize());
        if (fileSize != null && copiedBytes != fileSize) {
            // Check that file has been completely been copied
            i.remove();
            LOGGER.warn("Cannot completely download ({}/{}) data file (data object IP_ID: {}, file name: {})",
                        copiedBytes,
                        fileSize,
                        dataObjectIpId,
                        dataFile.getFilename());
            String downloadError = String.format("Cannot completely download data file from storage, only %d/%d bytes",
                                                 copiedBytes,
                                                 fileSize);
            downloadErrorFiles.add(Pair.of(dataFile, downloadError));
            dataFile.setDownloadError(downloadError);
        }
    }

    @Override
    public void downloadOrderMetalink(Long orderId, OutputStream os) throws ModuleException {
        Order order = orderRepository.findSimpleById(orderId);
        String tokenRequestParam = IOrderService.ORDER_TOKEN + "=" + orderHelperService.generateToken4PublicEndpoint(
            order);
        String scopeRequestParam = IOrderService.SCOPE + "=" + runtimeTenantResolver.getTenant();

        List<OrderDataFile> files = dataFileService.findAll(orderId);

        // Retrieve host for generating datafiles download urls
        FeignSecurityManager.asSystem();
        ResponseEntity<EntityModel<Project>> clientResponse = projectClient.retrieveProject(runtimeTenantResolver.getTenant());
        if (clientResponse == null
            || clientResponse.getBody() == null
            || clientResponse.getBody().getContent() == null
            || clientResponse.getBody().getContent().getHost() == null) {
            throw new ModuleException("Error retrieving project information from admin instance service");
        }
        Project project = clientResponse.getBody().getContent();
        String host = project.getHost();
        FeignSecurityManager.reset();

        // Create XML metalink object
        ObjectFactory factory = new ObjectFactory();
        MetalinkType xmlMetalink = factory.createMetalinkType();
        FilesType xmlFiles = factory.createFilesType();
        // For all data files
        for (OrderDataFile file : files) {
            FileType xmlFile = factory.createFileType();
            String filename = file.getFilename() != null ?
                file.getFilename() :
                file.getUrl().substring(file.getUrl().lastIndexOf('/') + 1);
            xmlFile.setIdentity(filename);
            xmlFile.setName(filename);
            if (file.getFilesize() != null) {
                xmlFile.setSize(BigInteger.valueOf(file.getFilesize()));
            }
            if (file.getMimeType() != null) {
                xmlFile.setMimetype(file.getMimeType().toString());
            }
            ResourcesType xmlResources = factory.createResourcesType();
            ResourcesType.Url xmlUrl = factory.createResourcesTypeUrl();
            // Build URL to publicdownloadFile
            String buff = host
                          + orderHelperService.buildUrl()
                          + OrderControllerEndpointConfiguration.ORDERS_PUBLIC_FILES_MAPPING
                          + "/"
                          + file.getId()
                          + "?"
                          + tokenRequestParam
                          + "&"
                          + scopeRequestParam;
            xmlUrl.setValue(buff);
            xmlResources.getUrl().add(xmlUrl);
            xmlFile.setResources(xmlResources);
            xmlFiles.getFile().add(xmlFile);
        }
        xmlMetalink.setFiles(xmlFiles);
        createXmlAndSendResponse(os, factory, xmlMetalink);
    }

    private void createXmlAndSendResponse(OutputStream os, ObjectFactory factory, MetalinkType xmlMetalink) {
        // Create XML and send reponse
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MetalinkType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // Format output
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Enable validation
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(METALINK_XML_SCHEMA_NAME);
            StreamSource xsdSource = new StreamSource(in);
            jaxbMarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                                                  .newSchema(xsdSource));

            // Marshall data
            jaxbMarshaller.marshal(factory.createMetalink(xmlMetalink), os);
            os.close();
        } catch (JAXBException | SAXException | IOException t) {
            LOGGER.error("Error while generating metalink order file", t);
            throw new RsRuntimeException(t);
        }
    }

}
