package com.example.e_trackr.utilities;

import java.io.Serializable;

public class File implements Serializable {

    public String fileName, borrowerName,timeStamp, fileDescription, id;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getFileName() {
        return fileName;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getFileDescription() {
        return fileDescription;
    }
}
