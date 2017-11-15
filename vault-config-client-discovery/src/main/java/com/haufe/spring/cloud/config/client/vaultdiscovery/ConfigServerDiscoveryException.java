package com.haufe.spring.cloud.config.client.vaultdiscovery;

/**
 * Exception that signals an error in the process of the config server discovery.
 */
public class ConfigServerDiscoveryException extends RuntimeException {


    /**
     * Constructs a new config server discovery exception with the specified detail message and
     * cause.
     * <p>
     * This constructor treats {@link Throwable#addSuppressed(Throwable) suppression} as being enabled and
     * the stack trace as being {@link Throwable#setStackTrace(StackTraceElement[]) writable}.
     *
     * @param msg the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public ConfigServerDiscoveryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
