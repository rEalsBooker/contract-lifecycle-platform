package com.contractguard.platform.contract;

public class FileObjectRow {
    private Long id;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getStorageKey() { return storageKey; } public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; } public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSizeBytes() { return sizeBytes; } public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}

