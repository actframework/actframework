package act.test;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.Act;
import act.conf.AppConfig;
import act.handler.builtin.FileGetter;
import act.test.req_modifier.RequestModifier;
import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.*;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.osgl.http.H.Header.Names.ACCEPT;
import static org.osgl.http.H.Header.Names.X_REQUESTED_WITH;
import static org.osgl.http.H.Method.POST;

class RequestBuilder {

    private static final RequestBody EMPTY_BODY = RequestBody.create(null, "");

    private Request.Builder builder;

    RequestBuilder(RequestSpec requestSpec, TestSession session, int port) {
        builder = new Request.Builder();
        String accept = requestSpec.accept;
        if (null != accept) {
            if (S.eq("json", accept, S.IGNORECASE)) {
                accept = H.Format.JSON.contentType();
            } else {
                H.Format format = H.Format.of(accept);
                if (null == format) {
                    format = H.Format.resolve(accept);
                }
                if (H.Format.UNKNOWN.isSameTypeWith(format)) {
                    throw new UnexpectedException("Invalid accept in request spec: " + accept);
                }
                accept = format.contentType();
            }
            builder.addHeader(ACCEPT, accept);
        }
        if ($.bool(requestSpec.ajax)) {
            builder.addHeader(X_REQUESTED_WITH, "XMLHttpRequest");
        }
        for (RequestModifier modifier : requestSpec.modifiers) {
            modifier.modifyRequest(builder);
        }
        if (null != session) {
            Headers lastHeaders = session.lastHeaders.get();
            if (null != lastHeaders) {
                String sessionHeader = Act.appConfig().sessionHeader();
                String val = lastHeaders.get(sessionHeader);
                if (S.notBlank(val)) {
                    builder.addHeader(sessionHeader, val);
                }
            }
        }
        for (Map.Entry<String, Object> entry : requestSpec.headers.entrySet()) {
            String headerName = entry.getKey();
            String headerVal = S.string(entry.getValue());
            if (null != session) {
                if (headerVal.startsWith("last:") || headerVal.startsWith("last|")) {
                    String payload = headerVal.substring(5);
                    if (S.blank(payload)) {
                        payload = headerName;
                    }
                    Headers headers = session.lastHeaders.get();
                    headerVal = null == headers ? null : S.string(session.lastHeaders.get().get(payload));
                }
            }
            if (null != headerVal) {
                builder.addHeader(headerName, headerVal);
            }
        }
        String reqUrl = requestSpec.url;
        if (null != session) {
            if (S.notBlank(session.scenario().urlContext) && !reqUrl.startsWith("/")) {
                reqUrl = S.pathConcat(session.scenario().urlContext, '/', reqUrl);
            }
        }
        String url = null == session ? reqUrl : session.processStringSubstitution(reqUrl);
        if (!reqUrl.startsWith("http")) {
            int portx = 0 != requestSpec.port ? requestSpec.port : port;
            url = S.concat("http://localhost:", portx, S.ensure(url).startWith("/"));
        }
        boolean hasParams = !requestSpec.params.isEmpty();
        if (hasParams) {
            processParamSubstitution(requestSpec.params, session);
        }
        boolean hasParts = !hasParams && POST == requestSpec.method && !requestSpec.parts.isEmpty();
        if (hasParts) {
            processParamSubstitution(requestSpec.parts, session);
        }
        switch (requestSpec.method) {
            case GET:
            case HEAD:
                if (hasParams) {
                    S.Buffer buf = S.buffer(url);
                    if (!url.contains("?")) {
                        buf.a("?__nil__=nil");
                    }
                    for (Map.Entry<String, Object> entry : requestSpec.params.entrySet()) {
                        String paramName = Codec.encodeUrl(entry.getKey());
                        String paramVal = Codec.encodeUrl(S.string(entry.getValue()));
                        buf.a("&").a(paramName).a("=").a(paramVal);
                    }
                    url = buf.toString();
                }
                builder.method(requestSpec.method.name(), null);
                break;
            case DELETE:
            case POST:
            case PUT:
            case PATCH:
                RequestBody body = EMPTY_BODY;
                String jsonBody = verifyJsonBody(requestSpec.json, session);
                String xmlBody = verifyXmlBody(requestSpec.xml, session);
                if (S.notBlank(jsonBody)) {
                    body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                } else if (S.notBlank(xmlBody)) {
                    body = RequestBody.create(MediaType.parse("text/xml"), xmlBody);
                } else if (hasParams) {
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    for (Map.Entry<String, Object> entry : requestSpec.params.entrySet()) {
                        formBuilder.add(entry.getKey(), S.string(entry.getValue()));
                    }
                    body = formBuilder.build();
                } else if (null != session && hasParts) {
                    MultipartBody.Builder formBuilder = new MultipartBody.Builder();
                    for (Map.Entry<String, Object> entry : requestSpec.parts.entrySet()) {
                        String key = entry.getKey();
                        Object obj = entry.getValue();
                        if (obj instanceof String) {
                            String val = S.string(entry.getValue());
                            byte[] content = null;
                            H.Format fileFormat = null;
                            String path = S.pathConcat("upload", '/', val);
                            File uploadFile = Act.app().testResource(path);
                            if (uploadFile.exists()) {
                                fileFormat = FileGetter.contentType(path);
                                content = IO.readContent(uploadFile);
                            } else {
                                path = S.pathConcat("test/upload", '/', val);
                                URL fileUrl = Act.getResource(path);
                                if (null != fileUrl) {
                                    String filePath = fileUrl.getFile();
                                    fileFormat = FileGetter.contentType(filePath);
                                    content = $.convert(fileUrl).to(byte[].class);
                                }
                            }
                            if (null != content) {
                                String checksum = IO.checksum(content);
                                RequestBody fileBody = RequestBody.create(MediaType.parse(fileFormat.contentType()), content);
                                String attachmentName = val.contains("/") ? S.cut(val).afterLast("/") : val;
                                formBuilder.addFormDataPart(key, attachmentName, fileBody);
                                session.cache("checksum-last", checksum);
                                session.cache("checksum-" + val, checksum);
                            } else {
                                formBuilder.addFormDataPart(key, val);
                            }
                        } else if (obj instanceof Collection) {
                            Collection col = (Collection) obj;
                            for (Object element : col) {
                                String val = S.string(element);
                                byte[] content = null;
                                H.Format fileFormat = null;
                                String path = S.pathConcat("upload", '/', val);
                                File uploadFile = Act.app().testResource(path);
                                if (uploadFile.exists()) {
                                    fileFormat = FileGetter.contentType(path);
                                    content = IO.readContent(uploadFile);
                                } else {
                                    path = S.pathConcat("test/upload", '/', val);
                                    URL fileUrl = Act.getResource(path);
                                    if (null != fileUrl) {
                                        String filePath = fileUrl.getFile();
                                        fileFormat = FileGetter.contentType(filePath);
                                        content = $.convert(fileUrl).to(byte[].class);
                                    }
                                }
                                if (null != content) {
                                    String checksum = IO.checksum(content);
                                    RequestBody fileBody = RequestBody.create(MediaType.parse(fileFormat.contentType()), content);
                                    String attachmentName = val.contains("/") ? S.cut(val).afterLast("/") : val;
                                    formBuilder.addFormDataPart(key, attachmentName, fileBody);
                                    session.cache("checksum-last", checksum);
                                    session.cache("checksum-" + val, checksum);
                                } else {
                                    formBuilder.addFormDataPart(key, val);
                                }
                            }
                        }
                    }
                    body = formBuilder.build();
                }
                builder.method((requestSpec.method.name()), body);
                break;
            default:
                throw E.unexpected("HTTP method not supported: " + requestSpec.method);
        }
        builder.url(url);
    }

    private void processParamSubstitution(Map<String, Object> params, TestSession session) {
        if (null == session) {
            return;
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof String) {
                String sVal = (String) val;
                if (sVal.startsWith("last:") || sVal.startsWith("last|")) {
                    String ref = sVal.substring(5);
                    entry.setValue(session.getLastVal(ref));
                } else if (sVal.contains("${")) {
                    sVal = session.processStringSubstitution(sVal);
                    entry.setValue(S.isInt(sVal) ? Long.parseLong(sVal) : sVal);
                }
            } else if (val instanceof Map) {
                processParamSubstitution((Map) val, session);
            } else if (val instanceof Collection) {
                val = processParamSubstitution((Collection) val, session);
                entry.setValue(val);
            }
        }
    }

    private Collection processParamSubstitution(Collection params, TestSession session) {
        Collection ret = Act.getInstance(params.getClass());
        for (Object val : params) {
            if (val instanceof String) {
                String sVal = (String) val;
                if (sVal.startsWith("last:") || sVal.startsWith("last|")) {
                    String ref = sVal.substring(5);
                    if (null != session) {
                        ret.add(session.getLastVal(ref));
                    }
                } else if (sVal.contains("${")) {
                    if (null != session) {
                        sVal = session.processStringSubstitution(sVal);
                    }
                    ret.add(S.isInt(sVal) ? Long.parseLong(sVal) : sVal);
                } else {
                    ret.add(sVal);
                }
            } else if (val instanceof Map) {
                processParamSubstitution((Map) val, session);
                ret.add(val);
            } else if (val instanceof Collection) {
                ret.add(processParamSubstitution((Collection) val, session));
            } else {
                ret.add(val);
            }
        }
        return ret;
    }

    Request build() {
        return builder.build();
    }

    private String verifyJsonBody(Object jsonBody, TestSession session) {
        if (jsonBody instanceof Map) {
            processParamSubstitution((Map) jsonBody, session);
        } else if (jsonBody instanceof Collection) {
            jsonBody = processParamSubstitution((Collection) jsonBody, session);
        }
        String s = null == jsonBody ? "" : (jsonBody instanceof String) ? (String) jsonBody : JSON.toJSONString(jsonBody);
        if (S.blank(s)) {
            return "";
        }
        final String origin = s;
        if (s.startsWith("resource:")) {
            s = S.ensure(s.substring(9).trim()).startWith("/");
            URL url = Act.getResource(s);
            E.unexpectedIf(null == url, "Cannot find JSON body: " + origin);
            s = IO.read(url).toString();
        }
        try {
            JSON.parse(s);
        } catch (Exception e) {
            E.unexpected(e, "Invalid JSON body: " + origin);
        }
        return s;
    }

    private String verifyXmlBody(Object xmlBody, TestSession session) {
        JSON json = null;
        if (xmlBody instanceof Map) {
            processParamSubstitution((Map) xmlBody, session);
            json = JSON.parseObject(JSON.toJSONString(xmlBody));
        } else if (xmlBody instanceof Collection) {
            xmlBody = processParamSubstitution((Collection) xmlBody, session);
            json = JSON.parseArray(JSON.toJSONString(xmlBody));
        }
        String s = null == xmlBody ? "" : (xmlBody instanceof String) ? (String) xmlBody : XML.toString($.convert(json).to(org.w3c.dom.Document.class));
        if (S.blank(s)) {
            return "";
        }
        final String origin = s;
        if (s.startsWith("resource:")) {
            s = S.ensure(s.substring(9).trim()).startWith("/");
            URL url = Act.getResource(s);
            E.unexpectedIf(null == url, "Cannot find JSON body: " + origin);
            s = IO.read(url).toString();
        }
        try {
            XML.read(s);
        } catch (Exception e) {
            E.unexpected(e, "Invalid JSON body: " + origin);
        }
        return s;
    }

}
