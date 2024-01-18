package com.example.e_trackr.utilities;

import java.io.Serializable;

public class File implements Serializable {

    public String fileName, borrowerName, timeStamp, fileStatus, fileDescription, id;
    public boolean outgoing, incoming;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setFileStatus(String fileStatus) {
        this.fileStatus = fileStatus;
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

    public String getFileStatus() {
        return fileStatus;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public void setOutgoing(boolean outgoing) {
        this.outgoing = outgoing;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }
}
