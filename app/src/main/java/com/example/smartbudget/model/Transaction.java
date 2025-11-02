package com.example.smartbudget.model;

public class Transaction {
    private int id;
    private String name;
    private double amount;
    private String note;
    private String date;
    private int walletId;
    private int subcategoryId;

    public Transaction(int id, String name, double amount, String note, String date, int walletId, int subcategoryId) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.walletId = walletId;
        this.subcategoryId = subcategoryId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public int getSubcategoryId() {
        return subcategoryId;
    }

    public void setSubCategoryId(int subCategoryId) {
        this.subcategoryId = subCategoryId;
    }
}
