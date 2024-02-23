/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.order;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsInFileException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.order.service.IBasketService;
import fr.cnes.regards.modules.order.service.utils.BasketSelectionFromFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test of PM 128 : Improvement of order basket : basket can be filled from a file.
 *
 * @author tguillou
 */
@ExtendWith(MockitoExtension.class)
public class BasketServiceAddSelectionTest {

    @Mock
    private IBasketService basketService;

    @Spy
    private BasketSelectionFromFileUtils basketSelectionUtils;

    @BeforeEach
    public void setUp() throws TooManyItemsSelectedInBasketException, CatalogSearchException, EmptySelectionException {
        basketService = Mockito.mock(BasketService.class);
        ReflectionTestUtils.setField(basketService, "authResolver", Mockito.mock(IAuthenticationResolver.class));
        ReflectionTestUtils.setField(basketService, "basketSelectionUtils", basketSelectionUtils);
        // lenient to avoid UnnecessaryStubbingException (mock is not always useful for all tests)
        Mockito.lenient().when(basketService.findOrCreate(Mockito.any())).thenReturn(new Basket());
        Mockito.lenient().when(basketService.addSelection(Mockito.any(), Mockito.any())).thenReturn(new Basket());
    }

    @Test
    public void test_basket_add_selection_from_file()
        throws TooManyItemsInFileException, TooManyItemsSelectedInBasketException, CatalogSearchException,
        EmptySelectionException {
        // GIVEN configure mock to call real method
        Mockito.when(basketService.addSelectionFromFile(Mockito.any())).thenCallRealMethod();
        ReflectionTestUtils.setField(basketSelectionUtils, "uploadFileMaxLines", 10);
        // GIVEN file with 3 providerId
        MockMultipartFile multipartFile = createMockFileWithProviderIds("id1", "id2", "encoreUnId");

        // WHEN add file to basket
        basketService.addSelectionFromFile(multipartFile);

        // THEN add selection is called with correct query.
        ArgumentCaptor<BasketSelectionRequest> request = ArgumentCaptor.forClass(BasketSelectionRequest.class);
        Mockito.verify(basketService).addSelection(Mockito.any(), request.capture());
        List<BasketSelectionRequest> allValues = request.getAllValues();
        Assertions.assertEquals(1, allValues.size());
        Assertions.assertEquals(1, allValues.get(0).getSearchParameters().get("q").size());
        Assertions.assertEquals("last=true AND providerId=(\"encoreUnId\" OR \"id2\" OR \"id1\")",
                                allValues.get(0).getSearchParameters().get("q").get(0));
    }

    @Test
    public void test_basket_add_selection_from_file_too_many_elements()
        throws TooManyItemsInFileException, TooManyItemsSelectedInBasketException, CatalogSearchException,
        EmptySelectionException {
        // GIVEN configure mock to call real method
        Mockito.when(basketService.addSelectionFromFile(Mockito.any())).thenCallRealMethod();
        ReflectionTestUtils.setField(basketSelectionUtils, "uploadFileMaxLines", 10);
        // GIVEN file with 3 providerId
        MockMultipartFile multipartFile = createMockFileWithNProviderIds(11);

        // WHEN add file to basket THEN method must fail
        Assertions.assertThrows(TooManyItemsInFileException.class,
                                () -> basketService.addSelectionFromFile(multipartFile));
    }

    @Test
    public void test_basket_add_selection_from_file_several_pages()
        throws TooManyItemsInFileException, TooManyItemsSelectedInBasketException, CatalogSearchException,
        EmptySelectionException {
        // GIVEN configure mock to call real method
        Mockito.when(basketService.addSelectionFromFile(Mockito.any())).thenCallRealMethod();
        ReflectionTestUtils.setField(basketSelectionUtils, "uploadFileMaxLines", 1000);
        // GIVEN file with 3 providerId
        MockMultipartFile multipartFile = createMockFileWithNProviderIds(500);

        // WHEN add file to basket THEN method must fail
        basketService.addSelectionFromFile(multipartFile);

        // THEN add selection is called 5 times because bulk size is set to 100 and we have 500 providerId in input
        ArgumentCaptor<BasketSelectionRequest> request = ArgumentCaptor.forClass(BasketSelectionRequest.class);
        Mockito.verify(basketService, Mockito.times(5)).addSelection(Mockito.any(), request.capture());
        List<BasketSelectionRequest> allValues = request.getAllValues();
        Assertions.assertEquals(5, allValues.size());
        // Then each add selection has only one request
        Assertions.assertEquals(1, allValues.get(0).getSearchParameters().get("q").size());
    }

    private static MockMultipartFile createMockFileWithProviderIds(String... providerIds) {
        String fileContent = String.join("\n", providerIds);
        String fileName = "filename.raw";
        return new MockMultipartFile(fileName, fileContent.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile createMockFileWithNProviderIds(int n) {
        String fileContent = IntStream.range(0, n)
                                      .mapToObj(String::valueOf)
                                      .map("providerId"::concat)
                                      .collect(Collectors.joining("\n"));
        String fileName = "filename.raw";
        return new MockMultipartFile(fileName, fileContent.getBytes(StandardCharsets.UTF_8));
    }
}
