package act.data;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.ActionContext;
import act.util.UploadFileStorageService;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.util.Closeable;
import org.apache.commons.fileupload.util.LimitedInputStream;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static act.data.MapUtil.mergeValueInMap;

/**
 * Disclaim: The code is modified from PlayFramework 1.3
 */
public class ApacheMultipartParser extends RequestBodyParser {

    @Override
    public Map<String, String[]> parse(ActionContext context) {
        H.Request request = context.req();
        InputStream body = request.inputStream();
        Map<String, String[]> result = new HashMap<>();
        try {
            FileItemIteratorImpl iter = new FileItemIteratorImpl(body, request.header("content-type"), request.characterEncoding());
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String fieldName = item.getFieldName();
                if (item.isFormField()) {
                    // must resolve encoding
                    String _encoding = request.characterEncoding(); // this is our default
                    String _contentType = item.getContentType();
                    if (_contentType != null) {
                        ContentTypeWithEncoding contentTypeEncoding = ContentTypeWithEncoding.parse(_contentType);
                        if (contentTypeEncoding.encoding != null) {
                            _encoding = contentTypeEncoding.encoding;
                        }
                    }
                    String val = IO.read(item.openStream()).encoding(Charset.forName(_encoding)).toString();
                    mergeValueInMap(result, fieldName, val);
                } else {
                    ISObject sobj = UploadFileStorageService.store(item, context.app());
                    context.addUpload(item.getFieldName(), sobj);
                    mergeValueInMap(result, fieldName, fieldName);
                }
            }
        } catch (FileUploadIOException e) {
            throw E.ioException("Error when handling upload", e);
        } catch (IOException e) {
            throw E.ioException("Error when handling upload", e);
        } catch (FileUploadException e) {
            throw E.ioException("Error when handling upload", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
        return result;
    }    // ---------------------------------------------------------- Class methods
    // ----------------------------------------------------- Manifest constants
    /**
     * HTTP content type header name.
     */
    private static final String CONTENT_TYPE = "Content-type";
    /**
     * HTTP content disposition header name.
     */
    private static final String CONTENT_DISPOSITION = "Content-disposition";
    /**
     * Content-disposition value for form data.
     */
    private static final String FORM_DATA = "form-data";
    /**
     * Content-disposition value for file attachment.
     */
    private static final String ATTACHMENT = "attachment";
    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";
    /**
     * HTTP content type header for multipart forms.
     */
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * HTTP content type header for multiple uploads.
     */
    private static final String MULTIPART_MIXED = "multipart/mixed";
    // ----------------------------------------------------------- Data members
    /**
     * The maximum size permitted for the complete request, as opposed to
     * {@link #fileSizeMax}. A value of -1 indicates no maximum.
     */
    private long sizeMax = -1;
    /**
     * The maximum size permitted for a single uploaded file, as opposed to
     * {@link #sizeMax}. A value of -1 indicates no maximum.
     */
    private long fileSizeMax = -1;

    // ------------------------------------------------------ Protected methods

    /**
     * Retrieves the boundary from the <code>Content-type</code> header.
     *
     * @param contentType The value of the content type header from which to extract the
     *                    boundary value.
     * @return The boundary, as a byte array.
     */
    private byte[] getBoundary(String contentType) {

        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map params = parser.parse(contentType, ';');
        String boundaryStr = (String) params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            boundary = boundaryStr.getBytes();
        }
        return boundary;
    }

    /**
     * Retrieves the file name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @return The file name for the current <code>encapsulation</code>.
     */
    private String getFileName(Map /* String, String */ headers) {
        String fileName = null;
        String cd = getHeader(headers, CONTENT_DISPOSITION);
        if (cd != null) {
            String cdl = cd.toLowerCase();
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                // Parameter parser can handle null input
                Map params = parser.parse(cd, ';');
                if (params.containsKey("filename")) {
                    fileName = (String) params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                        // IE7 returning fullpath name (#300920)
                        if (fileName.indexOf('\\') != -1) {
                            fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
                        }

                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

    /**
     * Retrieves the field name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @return The field name for the current <code>encapsulation</code>.
     */
    private String getFieldName(Map /* String, String */ headers) {
        String fieldName = null;
        String cd = getHeader(headers, CONTENT_DISPOSITION);
        if (cd != null && cd.toLowerCase().startsWith(FORM_DATA)) {

            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);
            // Parameter parser can handle null input
            Map params = parser.parse(cd, ';');
            fieldName = (String) params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }
        return fieldName;
    }

    /**
     * <p/>
     * Parses the <code>header-part</code> and returns as key/value pairs.
     * <p/>
     * <p/>
     * If there are multiple headers of the same names, the name will map to a
     * comma-separated list containing the values.
     *
     * @param headerPart The <code>header-part</code> of the current
     *                   <code>encapsulation</code>.
     * @return A <code>Map</code> containing the parsed HTTP request headers.
     */
    private Map /* String, String */ parseHeaders(String headerPart) {
        final int len = headerPart.length();
        Map<String, String> headers = new HashMap<String, String>();
        int start = 0;
        for (; ;) {
            int end = parseEndOfLine(headerPart, start);
            if (start == end) {
                break;
            }
            String header = headerPart.substring(start, end);
            start = end + 2;
            while (start < len) {
                int nonWs = start;
                while (nonWs < len) {
                    char c = headerPart.charAt(nonWs);
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                    ++nonWs;
                }
                if (nonWs == start) {
                    break;
                }
                // Continuation line found
                end = parseEndOfLine(headerPart, nonWs);
                header += " " + headerPart.substring(nonWs, end);
                start = end + 2;
            }
            parseHeaderLine(headers, header);
        }
        return headers;
    }

    /**
     * Skips bytes until the end of the current line.
     *
     * @param headerPart The headers, which are being parsed.
     * @param end        Index of the last byte, which has yet been processed.
     * @return Index of the \r\n sequence, which indicates end of line.
     */
    private int parseEndOfLine(String headerPart, int end) {
        int index = end;
        for (; ;) {
            int offset = headerPart.indexOf('\r', index);
            if (offset == -1 || offset + 1 >= headerPart.length()) {
                throw new IllegalStateException("Expected headers to be terminated by an empty line.");
            }
            if (headerPart.charAt(offset + 1) == '\n') {
                return offset;
            }
            index = offset + 1;
        }
    }

    /**
     * Reads the next header line.
     *
     * @param headers String with all headers.
     * @param header  Map where to store the current header.
     */
    private void parseHeaderLine(Map<String, String> headers, String header) {
        final int colonOffset = header.indexOf(':');
        if (colonOffset == -1) {
            // This header line is malformed, skip it.
            return;
        }
        String headerName = header.substring(0, colonOffset).trim().toLowerCase();
        String headerValue = header.substring(header.indexOf(':') + 1).trim();
        if (getHeader(headers, headerName) != null) {
            // More that one heder of that name exists,
            // append to the list.
            headers.put(headerName, getHeader(headers, headerName) + ',' + headerValue);
        } else {
            headers.put(headerName, headerValue);
        }
    }

    /**
     * Returns the header with the specified name from the supplied map. The
     * header lookup is case-insensitive.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @param name    The name of the header to return.
     * @return The value of specified header, or a comma-separated list if there
     *         were multiple headers of that name.
     */
    private final String getHeader(Map /* String, String */ headers, String name) {
        return (String) headers.get(name.toLowerCase());
    }

    /**
     * The iterator, which is returned by
     * {@link FileUploadBase#getItemIterator(RequestContext)}.
     */
    private class FileItemIteratorImpl implements FileItemIterator {

        /**
         * Default implementation of {@link FileItemStream}.
         */
        private class FileItemStreamImpl implements FileItemStream {

            /**
             * The file items content type.
             */
            private final String contentType;
            /**
             * The file items field name.
             */
            private final String fieldName;
            /**
             * The file items file name.
             */
            private final String name;
            /**
             * Whether the file item is a form field.
             */
            private final boolean formField;
            /**
             * The file items input stream.
             */
            private final InputStream stream;
            /**
             * Whether the file item was already opened.
             */
            private boolean opened;

            private FileItemHeaders fileItemHeaders;

            /**
             * CReates a new instance.
             *
             * @param pName        The items file name, or null.
             * @param pFieldName   The items field name.
             * @param pContentType The items content type, or null.
             * @param pFormField   Whether the item is a form field.
             */
            FileItemStreamImpl(String pName, String pFieldName, String pContentType, boolean pFormField) {
                name = pName;
                fieldName = pFieldName;
                contentType = pContentType;
                formField = pFormField;
                InputStream istream = multi.newInputStream();
                if (fileSizeMax != -1) {
                    istream = new LimitedInputStream(istream, fileSizeMax) {

                        protected void raiseError(long pSizeMax, long pCount) throws IOException {
                            FileUploadException e = new FileSizeLimitExceededException("The field " + fieldName + " exceeds its maximum permitted " + " size of " + pSizeMax + " characters.", pCount, pSizeMax);
                            throw new FileUploadIOException(e);
                        }
                    };
                }
                stream = istream;
            }

            public FileItemHeaders getHeaders() {
                return fileItemHeaders;
            }

            public void setHeaders(FileItemHeaders fileItemHeaders) {
                this.fileItemHeaders = fileItemHeaders;
            }


            /**
             * Returns the items content type, or null.
             *
             * @return Content type, if known, or null.
             */
            public String getContentType() {
                return contentType;
            }

            /**
             * Returns the items field name.
             *
             * @return Field name.
             */
            public String getFieldName() {
                return fieldName;
            }

            /**
             * Returns the items file name.
             *
             * @return File name, if known, or null.
             */
            public String getName() {
                return name;
            }

            /**
             * Returns, whether this is a form field.
             *
             * @return True, if the item is a form field, otherwise false.
             */
            public boolean isFormField() {
                return formField;
            }

            /**
             * Returns an input stream, which may be used to read the items
             * contents.
             *
             * @return Opened input stream.
             * @throws IOException An I/O error occurred.
             */
            public InputStream openStream() throws IOException {
                if (opened) {
                    throw new IllegalStateException("The stream was already opened.");
                }
                if (((Closeable) stream).isClosed()) {
                    throw new FileItemStream.ItemSkippedException();
                }
                return stream;
            }

            /**
             * Closes the file item.
             *
             * @throws IOException An I/O error occurred.
             */
            void close() throws IOException {
                stream.close();
            }
        }

        /**
         * The multi part stream to process.
         */
        private final MultipartStream multi;
        /**
         * The boundary, which separates the various parts.
         */
        private final byte[] boundary;
        /**
         * The item, which we currently process.
         */
        private FileItemStreamImpl currentItem;
        /**
         * The current items field name.
         */
        private String currentFieldName;
        /**
         * Whether we are currently skipping the preamble.
         */
        private boolean skipPreamble;
        /**
         * Whether the current item may still be read.
         */
        private boolean itemValid;
        /**
         * Whether we have seen the end of the file.
         */
        private boolean eof;

        /**
         * Creates a new instance.
         *
         * @throws FileUploadException An error occurred while parsing the request.
         * @throws IOException         An I/O error occurred.
         */
        FileItemIteratorImpl(InputStream input, String contentType, String charEncoding) throws FileUploadException, IOException {

            if ((null == contentType) || (!contentType.toLowerCase().startsWith(MULTIPART))) {
                throw new InvalidContentTypeException("the request doesn't contain a " + MULTIPART_FORM_DATA + " or " + MULTIPART_MIXED + " stream, content type header is " + contentType);
            }

            if (sizeMax >= 0) {
                // TODO check size

                input = new LimitedInputStream(input, sizeMax) {

                    protected void raiseError(long pSizeMax, long pCount) throws IOException {
                        FileUploadException ex = new SizeLimitExceededException("the request was rejected because" + " its size (" + pCount + ") exceeds the configured maximum" + " (" + pSizeMax + ")", pCount, pSizeMax);
                        throw new FileUploadIOException(ex);
                    }
                };

            }

            boundary = getBoundary(contentType);
            if (boundary == null) {
                throw new FileUploadException("the request was rejected because " + "no multipart boundary was found");
            }

            multi = new MultipartStream(input, boundary, 4096, null);
            multi.setHeaderEncoding(charEncoding);

            skipPreamble = true;
            findNextItem();
        }

        /**
         * Called for finding the nex item, if any.
         *
         * @return True, if an next item was found, otherwise false.
         * @throws IOException An I/O error occurred.
         */
        private boolean findNextItem() throws IOException {
            if (eof) {
                return false;
            }
            if (currentItem != null) {
                currentItem.close();
                currentItem = null;
            }
            for (; ;) {
                boolean nextPart;
                if (skipPreamble) {
                    nextPart = multi.skipPreamble();
                } else {
                    nextPart = multi.readBoundary();
                }
                if (!nextPart) {
                    if (currentFieldName == null) {
                        // Outer multipart terminated -> No more data
                        eof = true;
                        return false;
                    }
                    // Inner multipart terminated -> Return to parsing the outer
                    multi.setBoundary(boundary);
                    currentFieldName = null;
                    continue;
                }
                Map headers = parseHeaders(multi.readHeaders());
                if (currentFieldName == null) {
                    // We're parsing the outer multipart
                    String fieldName = getFieldName(headers);
                    if (fieldName != null) {
                        String subContentType = getHeader(headers, CONTENT_TYPE);
                        if (subContentType != null && subContentType.toLowerCase().startsWith(MULTIPART_MIXED)) {
                            currentFieldName = fieldName;
                            // Multiple files associated with this field name
                            byte[] subBoundary = getBoundary(subContentType);
                            multi.setBoundary(subBoundary);
                            skipPreamble = true;
                            continue;
                        }
                        String fileName = getFileName(headers);
                        currentItem = new FileItemStreamImpl(fileName, fieldName, getHeader(headers, CONTENT_TYPE), fileName == null);

                        itemValid = true;
                        return true;
                    }
                } else {
                    String fileName = getFileName(headers);
                    if (fileName != null) {
                        currentItem = new FileItemStreamImpl(fileName, currentFieldName, getHeader(headers, CONTENT_TYPE), false);
                        itemValid = true;
                        return true;
                    }
                }
                multi.discardBodyData();
            }
        }

        /**
         * Returns, whether another instance of {@link FileItemStream} is
         * available.
         *
         * @return True, if one or more additional file items are available,
         *         otherwise false.
         * @throws FileUploadException Parsing or processing the file item failed.
         * @throws IOException         Reading the file item failed.
         */
        public boolean hasNext() throws FileUploadException, IOException {
            return !eof && (itemValid || findNextItem());
        }

        /**
         * Returns the next available {@link FileItemStream}.
         *
         * @return FileItemStream instance, which provides access to the next
         *         file item.
         * @throws java.util.NoSuchElementException
         *                             No more items are available. Use {@link #hasNext()} to
         *                             prevent this exception.
         * @throws FileUploadException Parsing or processing the file item failed.
         * @throws IOException         Reading the file item failed.
         */
        public FileItemStream next() throws FileUploadException, IOException {
            if (eof || (!itemValid && !hasNext())) {
                throw new NoSuchElementException();
            }
            itemValid = false;
            return currentItem;
        }
    }

    /**
     * This exception is thrown for hiding an inner {@link FileUploadException}
     * in an {@link IOException}.
     */
    private static class FileUploadIOException extends IOException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -7047616958165584154L;
        /**
         * The exceptions cause; we overwrite the parent classes field, which is
         * available since Java 1.4 only.
         */
        private final FileUploadException cause;

        /**
         * Creates a <code>FileUploadIOException</code> with the given cause.
         *
         * @param pCause The exceptions cause, if any, or null.
         */
        public FileUploadIOException(FileUploadException pCause) {
            // We're not doing super(pCause) cause of 1.3 compatibility.
            cause = pCause;
        }

        /**
         * Returns the exceptions cause.
         *
         * @return The exceptions cause, if any, or null.
         */
        public Throwable getCause() {
            return cause;
        }
    }

    /**
     * Thrown to indicate that the request is not a multipart request.
     */
    private static class InvalidContentTypeException extends FileUploadException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -9073026332015646668L;

        /**
         * Constructs an <code>InvalidContentTypeException</code> with the
         * specified detail message.
         *
         * @param message The detail message.
         */
        public InvalidContentTypeException(String message) {
            super(message);
        }
    }

    /**
     * Thrown to indicate an IOException.
     */
    private static class IOFileUploadException extends FileUploadException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = 1749796615868477269L;
        /**
         * The exceptions cause; we overwrite the parent classes field, which is
         * available since Java 1.4 only.
         */
        private final IOException cause;

        /**
         * Creates a new instance with the given cause.
         *
         * @param pMsg       The detail message.
         * @param pException The exceptions cause.
         */
        public IOFileUploadException(String pMsg, IOException pException) {
            super(pMsg);
            cause = pException;
        }

        /**
         * Returns the exceptions cause.
         *
         * @return The exceptions cause, if any, or null.
         */
        public Throwable getCause() {
            return cause;
        }
    }

    /**
     * This exception is thrown, if a requests permitted size is exceeded.
     */
    protected abstract static class SizeException extends FileUploadException {

        /**
         * The actual size of the request.
         */
        private final long actual;
        /**
         * The maximum permitted size of the request.
         */
        private final long permitted;

        /**
         * Creates a new instance.
         *
         * @param message   The detail message.
         * @param actual    The actual number of bytes in the request.
         * @param permitted The requests size limit, in bytes.
         */
        protected SizeException(String message, long actual, long permitted) {
            super(message);
            this.actual = actual;
            this.permitted = permitted;
        }

        /**
         * Retrieves the actual size of the request.
         *
         * @return The actual size of the request.
         */
        public long getActualSize() {
            return actual;
        }

        /**
         * Retrieves the permitted size of the request.
         *
         * @return The permitted size of the request.
         */
        public long getPermittedSize() {
            return permitted;
        }
    }

    /**
     * Thrown to indicate that the request size exceeds the configured maximum.
     */
    private static class SizeLimitExceededException extends SizeException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -2474893167098052828L;

        /**
         * Constructs a <code>SizeExceededException</code> with the specified
         * detail message, and actual and permitted sizes.
         *
         * @param message   The detail message.
         * @param actual    The actual request size.
         * @param permitted The maximum permitted request size.
         */
        public SizeLimitExceededException(String message, long actual, long permitted) {
            super(message, actual, permitted);
        }
    }

    /**
     * Thrown to indicate that A files size exceeds the configured maximum.
     */
    private static class FileSizeLimitExceededException extends SizeException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = 8150776562029630058L;

        /**
         * Constructs a <code>SizeExceededException</code> with the specified
         * detail message, and actual and permitted sizes.
         *
         * @param message   The detail message.
         * @param actual    The actual request size.
         * @param permitted The maximum permitted request size.
         */
        public FileSizeLimitExceededException(String message, long actual, long permitted) {
            super(message, actual, permitted);
        }
    }
}
