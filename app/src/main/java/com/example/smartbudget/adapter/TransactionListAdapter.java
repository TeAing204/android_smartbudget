package com.example.smartbudget.adapter;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.example.smartbudget.R;
import com.example.smartbudget.model.Transaction;
import com.example.smartbudget.tmp.TransactionDisplay;

import java.util.List;

public class TransactionListAdapter extends ArrayAdapter<TransactionDisplay> {
    public TransactionListAdapter(Context context, List<TransactionDisplay> data) {
        super(context, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TransactionDisplay item = getItem(position);
        Transaction transaction = item.transaction;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, parent, false);
        }

        TextView tvSubcategoryName = convertView.findViewById(R.id.tvSubcategoryName);
        TextView tvTransactionName = convertView.findViewById(R.id.tvTransactionName);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);

        tvSubcategoryName.setText(item.subcategoryName + ": ");
        tvTransactionName.setText(item.transaction.getName());
        tvDate.setText(transaction.getDate());

        if (item.categoryId == 1) { // Chi tiêu
            tvAmount.setText(String.format("-%,.0f đ", transaction.getAmount()));
            tvAmount.setTextColor(getContext().getColor(R.color.red));
        } else { // Thu nhập
            tvAmount.setText(String.format("+%,.0f đ", transaction.getAmount()));
            tvAmount.setTextColor(getContext().getColor(R.color.blue));
        }

        return convertView;
    }
}