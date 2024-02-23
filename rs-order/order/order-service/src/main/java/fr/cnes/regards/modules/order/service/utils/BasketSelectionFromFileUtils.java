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
package fr.cnes.regards.modules.order.service.utils;

import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsInFileException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tguillou
 */
@Component
public class BasketSelectionFromFileUtils {

    public static final int UPLOAD_FILE_PRODUCT_ADDITION_BULK_SIZE = 100;

    @Value("${regards.order.basket.upload.file.max.lines:1000}")
    private long uploadFileMaxLines;

    /**
     * Return each line of a file.
     *
     * @throws TooManyItemsSelectedInBasketException if number of lines is greater than the configured max
     */
    public List<String> extractLinesOfFile(MultipartFile file) throws TooManyItemsInFileException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<String> providersIds = new ArrayList<>();
            String currentLine;
            while ((currentLine = buffer.readLine()) != null) {
                providersIds.add(currentLine.trim());
                if (providersIds.size() > uploadFileMaxLines) {
                    throw new TooManyItemsInFileException("Input file exceed max lines limit ("
                                                          + uploadFileMaxLines
                                                          + ")");
                }
            }
            return providersIds;
        } catch (IOException e) {
            throw new RsRuntimeException("Problem with multipart file. This case should never happen", e);
        }
    }
}
