package com.library.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单分页对象：记录当前页、每页条数、总记录数，并据此计算 SQL 的 LIMIT/OFFSET。
 */
public class Page<T> {

    private List<T> records = new ArrayList<T>();
    private int pageNo = 1;
    private int pageSize = 10;
    private long total = 0;

    public Page() {
    }

    public Page(int pageNo, int pageSize) {
        this.pageNo = pageNo < 1 ? 1 : pageNo;
        this.pageSize = pageSize < 1 ? 10 : pageSize;
    }

    /** OFFSET = (pageNo - 1) * pageSize */
    public int getOffset() {
        return (pageNo - 1) * pageSize;
    }

    public int getTotalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo < 1 ? 1 : pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? 10 : pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
