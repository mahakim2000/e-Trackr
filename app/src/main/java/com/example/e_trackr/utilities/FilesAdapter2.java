package com.example.e_trackr.utilities;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_trackr.databinding.ListItem2Binding;
import com.example.e_trackr.databinding.ListItemBinding;

import java.util.List;

public class FilesAdapter2 extends RecyclerView.Adapter<FilesAdapter2.MyViewHolder2> {

    private final List<File> files;
    private final FileListener fileListener;

    public FilesAdapter2(List<File> files, FileListener fileListener) {
        this.files = files;
        this.fileListener = fileListener;
    }

    @NonNull
    @Override
    public MyViewHolder2 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItem2Binding listItem2Binding = ListItem2Binding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder2(listItem2Binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder2 holder, int position) {
        holder.setFileData(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class MyViewHolder2 extends RecyclerView.ViewHolder {

        ListItem2Binding binding;

        MyViewHolder2(ListItem2Binding listItem2Binding) {
            super(listItem2Binding.getRoot());
            binding = listItem2Binding;
        }

        void setFileData(File file) {
            binding.fileName.setText(file.fileName);
            binding.fileDescription.setText(file.fileDescription);
            binding.getRoot().setOnClickListener(v -> fileListener.onFileClicked(file));
        }
    }
}
