package org.osgl.oms.xio.netty4;

import io.netty.handler.codec.http.HttpMethod;
import org.osgl.http.H;
import org.osgl.util.E;

import static io.netty.handler.codec.http.HttpMethod.*;

enum MethodConverter {
    ;
    static H.Method netty2osgl(HttpMethod nettyMethod) {
        if (eq(GET, nettyMethod)) {
            return H.Method.GET;
        } else if (eq(POST, nettyMethod)) {
            return H.Method.POST;
        } else if (eq(PUT, nettyMethod)) {
            return H.Method.PUT;
        } else if (eq(DELETE, nettyMethod)) {
            return H.Method.DELETE;
        } else if (eq(HEAD, nettyMethod)) {
            return H.Method.HEAD;
        } else if (eq(CONNECT, nettyMethod)) {
            return H.Method.CONNECT;
        } else if (eq(TRACE, nettyMethod)) {
            return H.Method.TRACE;
        } else if (eq(OPTIONS, nettyMethod)) {
            return H.Method.OPTIONS;
        }
        throw E.unexpected("Unknown Netty HttpMethod: %s", nettyMethod);
    }

    static HttpMethod osgl2netty(H.Method osglMethod) {
        switch (osglMethod) {
        case GET: return GET;
        case POST: return POST;
        case PUT: return PUT;
        case DELETE: return DELETE;
        case HEAD: return HEAD;
        case CONNECT: return CONNECT;
        case TRACE: return TRACE;
        case OPTIONS: return OPTIONS;
        default:
            throw E.unexpected("Unknown OSGL Http Method: %s", osglMethod);
        }
    }

    private static boolean eq(HttpMethod m1, HttpMethod m2) {
        return m1 == m2 || m2.equals(m1);
    }
}
