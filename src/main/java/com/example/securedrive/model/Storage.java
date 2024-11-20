package com.example.securedrive.model;

public class Storage {
    private String fullPath;
    private byte[] data;

    public Storage(String fullPath, byte[] data) {
        this.fullPath = fullPath;
        this.data = data;
    }

    // Getter ve Setter metotları
    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}


