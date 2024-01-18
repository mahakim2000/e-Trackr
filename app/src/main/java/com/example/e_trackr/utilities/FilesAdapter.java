package com.example.e_trackr.utilities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_trackr.databinding.ListItemBinding;

import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.MyViewHolder> {

    private final List<File> files;
    private final FileListener fileListener;

    public FilesAdapter(List<File> files, FileListener fileListener) {
        this.files = files;
        this.fileListener = fileListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemBinding listItemBinding = ListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder(listItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setFileData(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ListItemBinding binding;

        MyViewHolder(ListItemBinding listItemBinding) {
            super(listItemBinding.getRoot());
            binding = listItemBinding;
        }

        void setFileData(File file) {
            binding.fileName.setText(file.fileName);
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
