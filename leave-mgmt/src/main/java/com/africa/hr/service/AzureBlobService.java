package com.africa.hr.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AzureBlobService {

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient containerClient;

    public AzureBlobService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    public String generateUploadSasUrl(String blobName, String mimeType) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        BlobSasPermission permission = new BlobSasPermission().setWritePermission(true).setCreatePermission(true);
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, permission)
                .setProtocol(SasProtocol.HTTPS_ONLY)
                .setContentType(mimeType);
        String sasToken = blobClient.generateSas(values);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    public String generateDownloadSasUrl(String blobUrl) {
        String blobName = extractBlobName(blobUrl);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, permission)
                .setProtocol(SasProtocol.HTTPS_ONLY);
        String sasToken = blobClient.generateSas(values);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    public String buildBlobUrl(String blobName) {
        return containerClient.getBlobClient(blobName).getBlobUrl();
    }

    private String extractBlobName(String blobUrl) {
        String prefix = containerClient.getBlobContainerUrl() + "/";
        if (blobUrl.startsWith(prefix)) {
            return blobUrl.substring(prefix.length());
        }
        throw new IllegalArgumentException("Invalid blob URL: " + blobUrl);
    }
}