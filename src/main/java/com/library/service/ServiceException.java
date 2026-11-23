package com.library.service;

/** 业务层统一运行时异常：把底层 SQLException 包装成对 UI 友好的错误信息。 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
