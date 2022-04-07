package com.beemdevelopment.aegis.otp;

import android.net.Uri;

public class YandexAuthInfoException extends Exception {
    private final Uri _uri;

    public YandexAuthInfoException(Uri uri, Throwable cause) {
        super(cause);
        _uri = uri;
    }

    public YandexAuthInfoException(Uri uri, String message) {
        super(message);
        _uri = uri;
    }

    public YandexAuthInfoException(Uri uri, String message, Throwable cause) {
        super(message, cause);
        _uri = uri;
    }

    /**
     * Reports whether the scheme of the URI is phonefactor://.
     */
    public boolean isPhoneFactor() {
        return _uri != null && _uri.getPath() != null && _uri.getHost().contains("passport.yandex") && _uri.getPath().contains("qrsecure");
    }

    @Override
    public String getMessage() {
        Throwable cause = getCause();
        if (cause == null) {
            return super.getMessage();
        }

        return String.format("%s (%s)", super.getMessage(), cause.getMessage());
    }
}
