package com.example.bluechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private final String[] localDataSet;

    public CustomAdapter(String[] localDataSet) {
        this.localDataSet = localDataSet;
    }

    @NonNull
    @Override
    public CustomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.receive_msg_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.ViewHolder holder, int position) {
        holder.getTextView().setText(localDataSet[position]);

    }

    @Override
    public int getItemCount() {
        return localDataSet.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
//        private final TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView)  itemView.findViewById(R.id.chatting);
//            dateText = (TextView) itemView.findViewById(R.id.msgDateTime);
        }
        public TextView getTextView() {
            return textView;
        }
    }
}

