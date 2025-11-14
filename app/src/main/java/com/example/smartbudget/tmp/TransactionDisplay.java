package com.example.smartbudget.tmp;


import com.example.smartbudget.model.Transaction;

public class TransactionDisplay {
    public Transaction transaction;
    public String subcategoryName;
    public int categoryId;

    public TransactionDisplay(Transaction transaction, String subcategoryName, int categoryId) {
        this.transaction = transaction;
        this.subcategoryName = subcategoryName;
        this.categoryId = categoryId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
