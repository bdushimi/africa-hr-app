package com.africa.hr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.africa.hr.dto.s3.DownloadUrlRequest;
import com.africa.hr.dto.s3.DownloadUrlResponse;
import com.africa.hr.dto.s3.PresignedUrlRequest;
import com.africa.hr.dto.s3.PresignedUrlResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service implements S3ServiceInterface {

        private final S3Client s3Client;

        @Value("${aws.s3.bucket-name}")
        private String bucketName;

        @Value("${aws.s3.region}")
        private String region;

        @Value("${aws.s3.access-key}")
        private String accessKey;

        @Value("${aws.s3.secret-key}")
        private String secretKey;

        /**
         * Uploads a file to S3 and returns a downloadable URL
         *
         * @param file MultipartFile to upload
         * @return String containing the downloadable URL
         */
        public String uploadFile(MultipartFile file) throws IOException {
                // Generate a unique file name using UUID
                String fileName = generateUniqueFileName(file.getOriginalFilename());

                // Create the request
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .contentType(file.getContentType())
                                .build();

                // Upload the file
                PutObjectResponse response = s3Client.putObject(
                                putObjectRequest,
                                RequestBody.fromBytes(file.getBytes()));

                log.info("File uploaded successfully to S3: {}", fileName);

                // Return the URL of the uploaded file
                return getFileUrl(fileName);
        }

        /**
         * Generates a download URL for a file
         *
         * @param fileName The name of the file in S3
         * @return String URL to download the file
         */
        public String getFileUrl(String fileName) {
                return s3Client.utilities().getUrl(GetUrlRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .build()).toString();
        }

        /**
         * Generates a presigned download URL for a file with customizable options and
         * returns detailed information
         *
         * @param key       The key of the file in S3
         * @param expiresIn Optional expiration time in seconds (null for default 1
         *                  hour)
         * @param fileName  Optional custom filename for download (null for original)
         * @param inline    Whether to view inline in browser (true) or download (false)
         * @return Map containing the presigned URL and metadata
         */
        public Map<String, Object> generatePresignedDownloadUrl(String key, Integer expiresIn, String fileName,
                        Boolean inline) {
                try (S3Presigner presigner = S3Presigner.builder()
                                .region(software.amazon.awssdk.regions.Region.of(region))
                                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
                                                .create(
                                                                software.amazon.awssdk.auth.credentials.AwsBasicCredentials
                                                                                .create(accessKey, secretKey)))
                                .build()) {

                        // Get or detect actual file name if not provided
                        String actualFileName = fileName;
                        if (actualFileName == null || actualFileName.trim().isEmpty()) {
                                actualFileName = extractFileNameFromKey(key);
                        }

                        // Detect content type based on file name
                        String contentType = detectContentType(actualFileName);

                        // Set expiration time (default 1 hour if not specified)
                        Duration expiration = expiresIn != null
                                        ? Duration.ofSeconds(expiresIn)
                                        : Duration.ofHours(1);

                        // Calculate expiration timestamp
                        ZonedDateTime expiresAt = ZonedDateTime.now().plus(expiration);

                        // Start building the GetObject request
                        software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder requestBuilder = software.amazon.awssdk.services.s3.model.GetObjectRequest
                                        .builder()
                                        .bucket(bucketName)
                                        .key(key);

                        // Add response headers for custom filename or inline viewing if needed
                        if (actualFileName != null || Boolean.TRUE.equals(inline)) {
                                String contentDisposition;

                                if (Boolean.TRUE.equals(inline)) {
                                        contentDisposition = "inline";
                                } else {
                                        if (actualFileName != null && !actualFileName.trim().isEmpty()) {
                                                // Encode filename for content-disposition header
                                                String encodedFileName = actualFileName.replace("\"", "\\\"");
                                                contentDisposition = "attachment; filename=\"" + encodedFileName + "\"";
                                        } else {
                                                contentDisposition = "attachment";
                                        }
                                }

                                requestBuilder.responseContentDisposition(contentDisposition);
                        }

                        // Build the final request
                        software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = requestBuilder
                                        .build();

                        // Create the presign request with the configured expiration
                        software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
                                        .builder()
                                        .signatureDuration(expiration)
                                        .getObjectRequest(getObjectRequest)
                                        .build();

                        // Get the presigned URL
                        software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest = presigner
                                        .presignGetObject(presignRequest);
                        String presignedUrl = presignedRequest.url().toString();

                        log.info("Generated presigned download URL for key: {} (expires in: {} seconds)",
                                        key, expiration.getSeconds());

                        // Build and return the response
                        Map<String, Object> result = new HashMap<>();
                        result.put("downloadUrl", presignedUrl);
                        result.put("fileName", actualFileName);
                        result.put("contentType", contentType);
                        result.put("expiresAt", expiresAt);
                        result.put("expiresIn", expiration.getSeconds());

                        return result;
                }
        }

        /**
         * Generates a pre-signed URL for direct S3 upload
         * 
         * @param fileName      Original file name
         * @param contentType   Content type of the file
         * @param folder        Optional folder path in S3 (can be null)
         * @param expiresIn     Optional expiration time in seconds (can be null)
         * @param contentLength Optional expected content length (can be null)
         * @return Map containing the pre-signed URL and other upload data
         */
        public Map<String, String> generatePresignedUrl(String fileName, String contentType,
                        String folder, Integer expiresIn, Long contentLength) {
                try (S3Presigner presigner = S3Presigner.builder()
                                .region(software.amazon.awssdk.regions.Region.of(region))
                                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
                                                .create(
                                                                software.amazon.awssdk.auth.credentials.AwsBasicCredentials
                                                                                .create(accessKey, secretKey)))
                                .build()) {

                        // Generate a unique file key, prefixed with folder if provided
                        String fileKey;
                        if (folder != null && !folder.trim().isEmpty()) {
                                // Ensure folder ends with '/' if it doesn't already
                                String normalizedFolder = folder.endsWith("/") ? folder : folder + "/";
                                fileKey = normalizedFolder + generateUniqueFileName(fileName);
                        } else {
                                fileKey = generateUniqueFileName(fileName);
                        }

                        // Set expiration time for the URL (default 15 minutes if not specified)
                        Duration expiration = expiresIn != null
                                        ? Duration.ofSeconds(expiresIn)
                                        : Duration.ofMinutes(15);

                        // Build request
                        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(fileKey)
                                        .contentType(contentType);

                        // Add content length if provided
                        if (contentLength != null) {
                                requestBuilder.contentLength(contentLength);
                        }

                        PutObjectRequest objectRequest = requestBuilder.build();

                        // Generate the presigned request
                        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                                        .signatureDuration(expiration)
                                        .putObjectRequest(objectRequest)
                                        .build();

                        // Generate the presigned URL
                        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
                        String presignedUrl = presignedRequest.url().toString();

                        log.info("Pre-signed URL generated for file: {} with expiration {} seconds",
                                        fileKey, expiration.getSeconds());

                        // Create response map with necessary information
                        Map<String, String> response = new HashMap<>();
                        response.put("url", presignedUrl);
                        response.put("fileKey", fileKey);
                        response.put("downloadUrl", getFileUrl(fileKey));
                        response.put("expiresIn", String.valueOf(expiration.getSeconds()));

                        return response;
                }
        }

        /**
         * Generates a unique file name using UUID
         *
         * @param originalFilename The original file name
         * @return String containing the unique file name
         */
        private String generateUniqueFileName(String originalFilename) {
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                        fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                return UUID.randomUUID().toString() + fileExtension;
        }

        /**
         * Detects the content type of a file based on its extension
         * 
         * @param fileName The name of the file
         * @return The content type as a string
         */
        private String detectContentType(String fileName) {
                if (fileName == null) {
                        return "application/octet-stream";
                }

                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                        extension = fileName.substring(lastDot + 1).toLowerCase();
                }

                switch (extension) {
                        case "pdf":
                                return "application/pdf";
                        case "doc":
                        case "docx":
                                return "application/msword";
                        case "xls":
                        case "xlsx":
                                return "application/vnd.ms-excel";
                        case "ppt":
                        case "pptx":
                                return "application/vnd.ms-powerpoint";
                        case "jpg":
                        case "jpeg":
                                return "image/jpeg";
                        case "png":
                                return "image/png";
                        case "gif":
                                return "image/gif";
                        case "txt":
                                return "text/plain";
                        case "html":
                        case "htm":
                                return "text/html";
                        default:
                                return "application/octet-stream";
                }
        }

        /**
         * Extracts the original file name from an S3 key
         * 
         * @param key The S3 key
         * @return The original file name or a default name
         */
        private String extractFileNameFromKey(String key) {
                if (key == null) {
                        return "document";
                }

                // Try to get the original name after the UUID pattern
                int lastSlash = key.lastIndexOf('/');
                String fileName = lastSlash > 0 ? key.substring(lastSlash + 1) : key;

                // Check if the file follows our UUID naming pattern
                if (fileName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\..+")) {
                        return fileName.substring(37); // After UUID and dot
                }

                return fileName;
        }

        @Override
        public PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request) {
                // Implementation needed
                throw new UnsupportedOperationException("Method not implemented");
        }

        @Override
        public DownloadUrlResponse generatePresignedDownloadUrl(String objectKey, DownloadUrlRequest request) {
                // Implementation needed
                throw new UnsupportedOperationException("Method not implemented");
        }

        @Override
        public boolean deleteObject(String objectKey) {
                // Implementation needed
                throw new UnsupportedOperationException("Method not implemented");
        }
}