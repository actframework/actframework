package org.osgl.oms.xio.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.osgl.http.H;
import org.osgl.oms.RequestImplBase;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.E;

import java.io.InputStream;
import java.util.Set;

public class NettyRequest extends RequestImplBase<NettyRequest> {

    private FullHttpRequest nr;
    private ChannelHandlerContext nc;
    private HttpPostRequestDecoder postDecoder;
    private final HttpDataFactory fact = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

    public NettyRequest(FullHttpRequest nettyRequest, ChannelHandlerContext nettyContext, AppConfig config) {
        super(config);
        E.NPE(nettyRequest, nettyContext);
        nr = nettyRequest;
        nc = nettyContext;
    }

    @Override
    protected Class<NettyRequest> _impl() {
        return NettyRequest.class;
    }

    @Override
    protected H.Method _method() {
        return MethodConverter.netty2osgl(nr.getMethod());
    }

    @Override
    protected String _uri() {
        return nr.getUri();
    }

    @Override
    public String header(String name) {
        return nr.headers().get(name);
    }

    @Override
    public Iterable<String> headers(String name) {
        return nr.headers().getAll(name);
    }

    @Override
    protected String _ip() {
        return nc.channel().remoteAddress().toString();
    }

    @Override
    protected void _initCookieMap() {
        Set<Cookie> cookies = CookieDecoder.decode(header(H.Header.Names.COOKIE));
        for (Cookie c : cookies) {
            _setCookie(c.getName(), CookieConverter.netty2osgl(c));
        }
    }

    @Override
    public InputStream createInputStream() throws IllegalStateException {
        E.illegalStateIf(!H.Method.POST.equals(method()), "inputStream not supported on %s request", method());
        throw E.tbd();
    }

    @Override
    public String paramVal(String name) {
        throw E.tbd();
    }

    @Override
    public String[] paramVals(String name) {
        throw E.tbd();
    }

    @Override
    public Iterable<String> paramNames() {
        throw E.tbd();
    }
}
