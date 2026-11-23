package com.library.demo;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Fine;
import com.library.model.Reader;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.FineService;
import com.library.service.ReaderService;
import com.library.service.SearchService;
import com.library.util.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 演示模式 Service 集合：继承真实 Service 并覆盖方法，把数据操作转发给内存仓库 DemoData。
 *
 * <p>因为继承的是真实 Service 类型，UI 面板无需改动字段类型即可注入；DAO 三层、
 * 事务与索引基准代码完全不受影响。这些类仅用于演示模式与 GUI 自检。</p>
 */
public final class DemoServices {

    private DemoServices() {
    }

    public static class Books extends BookService {
        @Override
        public int createBook(Book book) {
            return DemoData.get().addBook(book);
        }

        @Override
        public boolean updateBook(Book book) {
            return DemoData.get().updateBook(book);
        }

        @Override
        public boolean deleteBook(int id) {
            return DemoData.get().deleteBook(id);
        }

        @Override
        public List<Book> listAll() {
            return DemoData.get().listBooks();
        }
    }

    public static class Readers extends ReaderService {
        @Override
        public int createReader(Reader reader) {
            return DemoData.get().addReader(reader);
        }

        @Override
        public boolean updateReader(Reader reader) {
            return DemoData.get().updateReader(reader);
        }

        @Override
        public boolean deleteReader(int id) {
            return DemoData.get().deleteReader(id);
        }

        @Override
        public List<Reader> listAll() {
            return DemoData.get().listReaders();
        }
    }

    public static class Borrows extends BorrowService {
        @Override
        public BorrowRecord borrow(int readerId, int bookId, String copyNo) {
            return DemoData.get().borrow(readerId, bookId, copyNo);
        }

        @Override
        public BigDecimal returnBook(int borrowId) {
            return DemoData.get().returnBook(borrowId);
        }

        @Override
        public LocalDate renew(int borrowId) {
            return DemoData.get().renew(borrowId);
        }

        @Override
        public List<BorrowRecord> listActive() {
            return DemoData.get().listActiveBorrows();
        }
    }

    public static class Search extends SearchService {
        @Override
        public Page<Book> searchBooks(String field, String keyword, int pageNo, int pageSize) {
            return DemoData.get().searchBooks(field, keyword, pageNo, pageSize);
        }
    }

    public static class Fines extends FineService {
        @Override
        public List<Fine> listUnpaid() {
            return DemoData.get().listUnpaidFines();
        }

        @Override
        public boolean pay(int fineId) {
            return DemoData.get().payFine(fineId);
        }
    }
}
