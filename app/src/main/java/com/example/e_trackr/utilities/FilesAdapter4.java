package com.example.e_trackr.utilities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_trackr.databinding.ListItem4Binding;

import java.util.List;

public class FilesAdapter4 extends RecyclerView.Adapter<FilesAdapter4.MyViewHolder4> {

    private final List<File> files;
    private final FileListener fileListener;

    public FilesAdapter4(List<File> files, FileListener fileListener) {
        this.files = files;
        this.fileListener = fileListener;
    }

    @NonNull
    @Override
    public MyViewHolder4 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItem4Binding listItem4Binding = ListItem4Binding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder4(listItem4Binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder4 holder, int position) {
        holder.setFileData(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class MyViewHolder4 extends RecyclerView.ViewHolder {

        ListItem4Binding binding;

        MyViewHolder4(ListItem4Binding listItem4Binding) {
            super(listItem4Binding.getRoot());
            binding = listItem4Binding;
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
