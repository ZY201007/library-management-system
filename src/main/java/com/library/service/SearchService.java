package com.library.service;

import com.library.dao.BookDao;
import com.library.dao.ReaderDao;
import com.library.model.Book;
import com.library.model.Reader;
import com.library.util.Page;

import java.sql.SQLException;
import java.util.List;

/** 检索业务层：参数化查询 + LIMIT/OFFSET 分页，配合检索字段索引。 */
public class SearchService {

    private final BookDao bookDao = new BookDao();
    private final ReaderDao readerDao = new ReaderDao();

    /** 图书检索：返回带 total/总页数的分页对象。 */
    public Page<Book> searchBooks(String field, String keyword, int pageNo, int pageSize) {
        Page<Book> page = new Page<Book>(pageNo, pageSize);
        try {
            page.setTotal(bookDao.count(field, keyword));
            List<Book> records = bookDao.search(field, keyword, page.getOffset(), page.getPageSize());
            page.setRecords(records);
            return page;
        } catch (SQLException e) {
            throw new ServiceException("图书检索失败: " + e.getMessage(), e);
        }
    }

    /** 读者检索。 */
    public Page<Reader> searchReaders(String field, String keyword, int pageNo, int pageSize) {
        Page<Reader> page = new Page<Reader>(pageNo, pageSize);
        try {
            page.setTotal(readerDao.count(field, keyword));
            page.setRecords(readerDao.search(field, keyword, page.getOffset(), page.getPageSize()));
            return page;
        } catch (SQLException e) {
            throw new ServiceException("读者检索失败: " + e.getMessage(), e);
        }
    }
}
