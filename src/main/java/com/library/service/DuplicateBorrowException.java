package com.library.service;

/** 同一馆藏副本已被借出（唯一约束 uk_active_copy 冲突）。 */
public class DuplicateBorrowException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public DuplicateBorrowException(String message) {
        super(message);
    }

    public DuplicateBorrowException(String message, Throwable cause) {
        super(message, cause);
    }
}
