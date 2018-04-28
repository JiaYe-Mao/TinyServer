package org.mao.tinyserver.exception;

public class IllegalRequestException extends Exception {
    public IllegalRequestException(String msg) {
        super(msg);
    }
}
