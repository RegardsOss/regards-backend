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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.swagger.autoconfigure;

import com.google.gson.JsonParser;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;

/**
 * Intercept OpenAPI to reformat API documentation properly with GSON serialization
 *
 * @author Marc SORDI
 */
public class OpenApiFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ByteResponseWrapper byteResponseWrapper = new ByteResponseWrapper((HttpServletResponse) response);
        ByteRequestWrapper byteRequestWrapper = new ByteRequestWrapper((HttpServletRequest) request);
        chain.doFilter(byteRequestWrapper, byteResponseWrapper);
        String jsonResponse = new String(byteResponseWrapper.getBytes(), response.getCharacterEncoding());
        response.getOutputStream()
                .write((JsonParser.parseString(jsonResponse)).toString().getBytes(response.getCharacterEncoding()));
    }

    static class OpenApiResponseWrapper extends HttpServletResponseWrapper {

        private final CharArrayWriter output;

        public OpenApiResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new CharArrayWriter();
        }

        public String getResponseContent() {
            return output.toString();
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }

    }

    @Override
    public void destroy() {
        // Nothing to do
    }

    static class ByteResponseWrapper extends HttpServletResponseWrapper {

        private final PrintWriter writer;

        private final ByteOutputStream output;

        public byte[] getBytes() {
            writer.flush();
            return output.getBytes();
        }

        public ByteResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new ByteOutputStream();
            writer = new PrintWriter(output);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return output;
        }
    }

    static class ByteRequestWrapper extends HttpServletRequestWrapper {

        private final ByteInputStream byteInputStream;

        public ByteRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream inputStream = request.getInputStream();

            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            byteInputStream = new ByteInputStream(new ByteArrayInputStream(baos.toByteArray()));
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() {
            return byteInputStream;
        }

    }

    static class ByteOutputStream extends ServletOutputStream {

        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            bos.write(b);
        }

        public byte[] getBytes() {
            return bos.toByteArray();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Nothing to do
        }
    }

    static class ByteInputStream extends ServletInputStream {

        private final InputStream inputStream;

        public ByteInputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Nothing to do.
        }
    }
}
