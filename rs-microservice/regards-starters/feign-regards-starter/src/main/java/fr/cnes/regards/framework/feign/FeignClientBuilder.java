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
package fr.cnes.regards.framework.feign;

import com.google.gson.Gson;
import feign.Client;
import feign.Feign;
import feign.RequestInterceptor;
import feign.Target;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBDecoder;
import feign.jaxb.JAXBEncoder;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;

import java.util.Arrays;

/**
 * Helper class for building Feign client programmatically
 * @author Marc Sordi
 */
public final class FeignClientBuilder {

    private FeignClientBuilder() {
    }

    /**
     * Generate client
     * @param pTarget Target to add informations in header like Autorization.
     * @return IResourcesClient a client instance
     */
    public static <T> T build(final Target<T> pTarget) {
        return Feign.builder() // Feign customization
                .encoder(new GsonEncoder()).decoder(new ResponseEntityDecoder(new GsonDecoder()))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
                .target(pTarget);
    }

    /**
     * Generate client
     * @param pTarget Target to add informations in header like Autorization.
     * @return IResourcesClient a client instance
     */
    public static <T> T build(final Target<T> pTarget, Gson gson) {
        return Feign.builder() // Feign customization
                .encoder(new GsonEncoder(gson)).decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
                .target(pTarget);
    }

    /**
     * Generate client
     * @param pTarget Target to add informations in header like Autorization.
     * @param requestInterceptors Add custom headers to all requests
     * @return IResourcesClient a client instance
     */
    public static <T> T build(final Target<T> pTarget, Gson gson, RequestInterceptor... requestInterceptors) {
        return Feign.builder().requestInterceptors(Arrays.asList(requestInterceptors)) // Feign customization
                .encoder(new GsonEncoder(gson)).decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
                .target(pTarget);
    }

    /**
     * Generate client
     * @param pTarget Target to add informations in header like Autorization.
     * @return IResourcesClient a client instance
     */
    public static <T> T build(final Target<T> pTarget, Client client, Gson gson) {
        return Feign.builder().client(client) // Feign customization
                .encoder(new GsonEncoder(gson)).decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
                .target(pTarget);
    }

    /**
     * Generate client
     * @param pTarget Target to add informations in header like Autorization.
     * @return IResourcesClient a client instance
     */
    public static <T> T buildXml(final Target<T> pTarget, Client client) {
        JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder().withMarshallerJAXBEncoding("UTF-8").build();
        return Feign.builder().client(client) // Feign customization
                .encoder(new JAXBEncoder(jaxbFactory)).decoder(new ResponseEntityDecoder(new JAXBDecoder(jaxbFactory)))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
                .target(pTarget);
    }
}
