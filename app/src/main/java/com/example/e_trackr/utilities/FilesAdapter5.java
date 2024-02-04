package com.example.e_trackr.utilities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_trackr.databinding.ListItem5Binding;

import java.util.List;

public class FilesAdapter5 extends RecyclerView.Adapter<FilesAdapter5.MyViewHolder5> {

    private final List<File> files;
    private final FileListener fileListener;

    public FilesAdapter5(List<File> files, FileListener fileListener) {
        this.files = files;
        this.fileListener = fileListener;
    }

    @NonNull
    @Override
    public MyViewHolder5 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItem5Binding listItem5Binding = ListItem5Binding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder5(listItem5Binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder5 holder, int position) {
        holder.setFileData(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class MyViewHolder5 extends RecyclerView.ViewHolder {

        ListItem5Binding binding;

        MyViewHolder5(ListItem5Binding listItem5Binding) {
            super(listItem5Binding.getRoot());
            binding = listItem5Binding;
        }

        void setFileData(File file) {
            binding.fileName.setText(file.fileName);
            binding.fileDescription.setText(file.fileDescription);

            String fileStatus;
            if (file.outgoing) {
                fileStatus = "Outgoing";
            } else if (file.incoming) {
                fileStatus = "Returned";
            } else {
                fileStatus = "Unknown Status";
            }
            binding.fileStatus.setText(fileStatus);

            binding.getRoot().setOnClickListener(v -> fileListener.onFileClicked(file));
        }
    }
}
