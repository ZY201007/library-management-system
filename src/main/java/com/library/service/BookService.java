package com.library.service;

import com.library.dao.BookDao;
import com.library.model.Book;

import java.sql.SQLException;
import java.util.List;

/** 图书业务层：建档、修改、删除、查询。 */
public class BookService {

    private final BookDao bookDao = new BookDao();

    /** 图书建档（功能点：新增图书）。 */
    public int createBook(Book book) {
        try {
            return bookDao.insert(book);
        } catch (SQLException e) {
            throw new ServiceException("图书建档失败: " + e.getMessage(), e);
        }
    }

    public boolean updateBook(Book book) {
        try {
            return bookDao.update(book) == 1;
        } catch (SQLException e) {
            throw new ServiceException("修改图书失败: " + e.getMessage(), e);
        }
    }

    public boolean deleteBook(int id) {
        try {
            return bookDao.delete(id) == 1;
        } catch (SQLException e) {
            throw new ServiceException("删除图书失败（可能存在借阅记录引用）: " + e.getMessage(), e);
        }
    }

    public Book getBook(int id) {
        try {
            return bookDao.findById(id);
        } catch (SQLException e) {
            throw new ServiceException("查询图书失败: " + e.getMessage(), e);
        }
    }

    public List<Book> listAll() {
        try {
            return bookDao.listAll();
        } catch (SQLException e) {
            throw new ServiceException("查询图书列表失败: " + e.getMessage(), e);
        }
    }
}
