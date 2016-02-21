package com.getkeepsafe.relinker;

/**
 * Created by xiphi_000 on 2/20/2016.
 */
public class MissingLibraryException extends RuntimeException {
    public MissingLibraryException(final String library) {
        super(library);
    }
}