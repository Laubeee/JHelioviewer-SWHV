package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

/**
 * An enum with the some frequently used HTTP message headers. These are
 * documented in the RFC.
 * 
 * @author caplins
 */
public enum HTTPHeaderKey {

    CACHE_CONTROL("Cache-Control"),
    CONNECTION("Connection"),
    TRANSFER_ENCODING("Transfer-Encoding"),
    HOST("Host"),
    USER_AGENT("User-Agent"),
    ACCEPT_ENCODING("Accept-Encoding"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_TYPE("Content-Type");

    private final String str;

    HTTPHeaderKey(String _str) {
        str = _str;
    }

    @Override
    public String toString() {
        return str;
    }

}
