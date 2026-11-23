package com.library.service;

import com.library.dao.ReaderDao;
import com.library.model.Reader;

import java.sql.SQLException;
import java.util.List;

/** 读者业务层：读者建档与 CRUD 管理。 */
public class ReaderService {

    private final ReaderDao readerDao = new ReaderDao();

    /** 读者建档。 */
    public int createReader(Reader reader) {
        if (reader.getReaderNo() == null || reader.getReaderNo().trim().isEmpty()) {
            throw new ServiceException("读者证号不能为空");
        }
        try {
            return readerDao.insert(reader);
        } catch (SQLException e) {
            throw new ServiceException("读者建档失败（证号可能重复）: " + e.getMessage(), e);
        }
    }

    public boolean updateReader(Reader reader) {
        try {
            return readerDao.update(reader) == 1;
        } catch (SQLException e) {
            throw new ServiceException("修改读者失败: " + e.getMessage(), e);
        }
    }

    public boolean deleteReader(int id) {
        try {
            return readerDao.delete(id) == 1;
        } catch (SQLException e) {
            throw new ServiceException("删除读者失败（可能存在借阅/罚款记录引用）: " + e.getMessage(), e);
        }
    }

    public Reader getReader(int id) {
        try {
            return readerDao.findById(id);
        } catch (SQLException e) {
            throw new ServiceException("查询读者失败: " + e.getMessage(), e);
        }
    }

    public Reader getByReaderNo(String readerNo) {
        try {
            return readerDao.findByReaderNo(readerNo);
        } catch (SQLException e) {
            throw new ServiceException("按证号查询读者失败: " + e.getMessage(), e);
        }
    }

    public List<Reader> listAll() {
        try {
            return readerDao.listAll();
        } catch (SQLException e) {
            throw new ServiceException("查询读者列表失败: " + e.getMessage(), e);
        }
    }
}
