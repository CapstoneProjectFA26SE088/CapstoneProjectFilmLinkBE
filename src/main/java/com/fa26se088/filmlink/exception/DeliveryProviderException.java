package com.fa26se088.filmlink.exception;

/** Lỗi chung khi gọi nhà vận chuyển (timeout, 5xx, từ chối đơn...). Adapter map mọi lỗi về đây. */
public class DeliveryProviderException extends RuntimeException {
    private final String provider;

    public DeliveryProviderException(String provider, String message) {
        super("[" + provider + "] " + message);
        this.provider = provider;
    }

    public DeliveryProviderException(String provider, String message, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
    }

    public String getProvider() { return provider; }
}
