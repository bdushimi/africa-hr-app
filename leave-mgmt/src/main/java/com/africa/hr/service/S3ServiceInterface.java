package com.africa.hr.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.africa.hr.dto.s3.DownloadUrlRequest;
import com.africa.hr.dto.s3.DownloadUrlResponse;
import com.africa.hr.dto.s3.PresignedUrlRequest;
import com.africa.hr.dto.s3.PresignedUrlResponse;

public interface S3ServiceInterface {

    /**
     * Upload a file to S3
     * 
     * @param file the MultipartFile to upload
     * @return the URL of the uploaded file
     * @throws IOException if there's an issue with file handling
     */
    String uploadFile(MultipartFile file) throws IOException;

    /**
     * Get a direct URL for a file in S3
     * 
     * @param fileName the name of the file in S3
     * @return the URL of the file
     */
    String getFileUrl(String fileName);

    /**
     * Generate a presigned URL for uploading a file to S3
     * 
     * @param fileName      the name of the file to upload
     * @param contentType   the content type of the file
     * @param folder        the folder to upload to (optional)
     * @param expiresIn     expiration time in seconds
     * @param contentLength the content length in bytes (optional)
     * @return Map containing url, fileKey, downloadUrl, and expiresIn
     */
    Map<String, String> generatePresignedUrl(String fileName, String contentType, String folder,
            Integer expiresIn, Long contentLength);

    /**
     * Generate a presigned URL for downloading a file from S3
     * 
     * @param objectKey the key of the object in S3
     * @param expiresIn expiration time in seconds
     * @param fileName  custom filename for download (optional)
     * @param inline    whether to display inline in browser
     * @return Map containing downloadUrl, fileName, contentType, and expiresAt
     */
    Map<String, Object> generatePresignedDownloadUrl(String objectKey, Integer expiresIn,
            String fileName, Boolean inline);

    /**
     * Generate a presigned URL for uploading a file to S3
     * 
     * @param request contains fileName, contentType, expiresIn (seconds), folder
     * @return response with presignedUrl, objectKey, fileName, contentType,
     *         expiresAt
     */
    PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request);

    /**
     * Generate a presigned URL for downloading a file from S3
     * 
     * @param objectKey the key of the object in S3
     * @param request   contains fileName (optional), expiresIn (seconds), inline
     *                  (boolean)
     * @return response with downloadUrl, fileName, contentType, expiresAt
     */
    DownloadUrlResponse generatePresignedDownloadUrl(String objectKey, DownloadUrlRequest request);

    /**
     * Delete an object from S3
     * 
     * @param objectKey the key of the object in S3
     * @return true if deletion was successful
     */
    boolean deleteObject(String objectKey);
}