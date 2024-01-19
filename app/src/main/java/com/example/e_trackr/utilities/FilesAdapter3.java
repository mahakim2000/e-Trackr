package com.example.e_trackr.utilities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_trackr.databinding.ListItem3Binding;

import java.util.List;

public class FilesAdapter3 extends RecyclerView.Adapter<FilesAdapter3.MyViewHolder3> {

    private final List<File> files;
    private final FileListener fileListener;

    public FilesAdapter3(List<File> files, FileListener fileListener) {
        this.files = files;
        this.fileListener = fileListener;
    }

    @NonNull
    @Override
    public MyViewHolder3 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItem3Binding listItem3Binding = ListItem3Binding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder3(listItem3Binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder3 holder, int position) {
        holder.setFileData(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class MyViewHolder3 extends RecyclerView.ViewHolder {

        ListItem3Binding binding;

        MyViewHolder3(ListItem3Binding listItem3Binding) {
            super(listItem3Binding.getRoot());
            binding = listItem3Binding;
        }

        void setFileData(File file) {
            binding.fileName.setText(file.fileName);
            binding.fileDescription.setText(file.fileDescription);
            binding.borrowerName.setText(file.borrowerName);
            binding.timeStamp.setText(file.timeStamp);

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
