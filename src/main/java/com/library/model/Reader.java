package com.library.model;

/** 读者实体：对应 reader 表。 */
public class Reader {

    private Integer id;
    private String readerNo;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String readerType;
    private Integer maxBorrow;

    public Reader() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReaderNo() {
        return readerNo;
    }

    public void setReaderNo(String readerNo) {
        this.readerNo = readerNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReaderType() {
        return readerType;
    }

    public void setReaderType(String readerType) {
        this.readerType = readerType;
    }

    public Integer getMaxBorrow() {
        return maxBorrow;
    }

    public void setMaxBorrow(Integer maxBorrow) {
        this.maxBorrow = maxBorrow;
    }

    @Override
    public String toString() {
        return "Reader{id=" + id + ", readerNo='" + readerNo + "', name='" + name + "'}";
    }
}
