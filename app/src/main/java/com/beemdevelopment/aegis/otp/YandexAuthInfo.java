package com.beemdevelopment.aegis.otp;

import android.net.Uri;

import java.io.Serializable;

public class YandexAuthInfo implements Serializable {
    public static final String SCHEME = "https";
    public static final String HOST_PART = "passport.yandex";
    public static final String PATH_PART = "qrsecure";
    public static final String TRACK_ID = "track_id";
    public static final String MAGIC_ID = "magic";

    private String _host;
    private String _track_id;
    private String _magic_id;
    private String _code;

    public YandexAuthInfo() {}

    public YandexAuthInfo(String host,String track_id, String magic_id, String code) {
        _host = host;
        _track_id = track_id;
        _magic_id = magic_id;
        _code = code;
    }

    public YandexAuthInfo(YandexAuthInfo info) {
        _host = info._host;
        _track_id = info._track_id;
        _magic_id = info._magic_id;
        _code = info._code;
    }

    public static YandexAuthInfo parseUri(String s, String code) throws YandexAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new YandexAuthInfoException(uri, String.format("Bad URI format: %s", s));
        }
        return YandexAuthInfo.parseUri(uri, code);
    }

    public static YandexAuthInfo parseUri(Uri uri, String code) throws YandexAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME)) {
            throw new YandexAuthInfoException(uri, String.format("Unsupported protocol: %s", scheme));
        }

        String host = uri.getHost();
        if (host == null || !host.contains(HOST_PART)) {
            throw new YandexAuthInfoException(uri, String.format("Bad host: %s", host));
        }

        String path = uri.getPath();
        if (path == null || !path.contains(PATH_PART)) {
            throw new YandexAuthInfoException(uri, String.format("Unsupported path: %s", path));
        }

        // 'track_id' is a required parameter
        String track_id = uri.getQueryParameter(TRACK_ID);
        if (track_id == null) {
            throw new YandexAuthInfoException(uri, "Parameter 'track_id' is not present");
        }

        // 'magic' is a required parameter
        String magic = uri.getQueryParameter(MAGIC_ID);
        if (magic == null) {
            throw new YandexAuthInfoException(uri, "Parameter 'magic' is not present");
        }


        return new YandexAuthInfo(host, track_id, magic, code);
    }

    public String getTrackId() {
        return _track_id;
    }

    public String getMagicId() {
        return _magic_id;
    }

    public String getOTP(){
        return _code;
    }

    public void setOTP(String otp){
        _code = otp;
    }
}
