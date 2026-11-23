package com.library.service;

import com.library.dao.FineDao;
import com.library.model.Fine;

import java.sql.SQLException;
import java.util.List;

/** 罚款业务层：账单查询与缴费。逾期金额本身由 FineCalculator 在归还时计算。 */
public class FineService {

    private final FineDao fineDao = new FineDao();

    /** 查询所有未缴账单。 */
    public List<Fine> listUnpaid() {
        try {
            return fineDao.listUnpaid();
        } catch (SQLException e) {
            throw new ServiceException("查询未缴罚款失败: " + e.getMessage(), e);
        }
    }

    public List<Fine> listByReader(int readerId) {
        try {
            return fineDao.listByReader(readerId);
        } catch (SQLException e) {
            throw new ServiceException("查询读者罚款失败: " + e.getMessage(), e);
        }
    }

    /** 缴纳罚款。 */
    public boolean pay(int fineId) {
        try {
            return fineDao.markPaid(fineId) == 1;
        } catch (SQLException e) {
            throw new ServiceException("缴纳罚款失败: " + e.getMessage(), e);
        }
    }
}
