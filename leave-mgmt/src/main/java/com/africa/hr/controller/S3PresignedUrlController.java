package com.africa.hr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.africa.hr.dto.PresignedUrlRequestDto;
import com.africa.hr.dto.PresignedUrlResponseDto;
import com.africa.hr.dto.DownloadUrlRequestDto;
import com.africa.hr.dto.DownloadUrlResponseDto;
import com.africa.hr.service.S3ServiceInterface;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/s3")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "S3 Operations", description = "API for S3 operations including pre-signed URL generation")
public class S3PresignedUrlController {

        private final S3ServiceInterface s3Service;

        @PostMapping("/presigned-url")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
        @Operation(summary = "Generate a pre-signed URL for S3 upload", description = "Generates a pre-signed URL for direct upload to S3 from the browser", responses = {
                        @ApiResponse(responseCode = "200", description = "Pre-signed URL generated successfully", content = @Content(schema = @Schema(implementation = PresignedUrlResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "500", description = "Server error")
        })
        public ResponseEntity<PresignedUrlResponseDto> generatePresignedUrl(PresignedUrlRequestDto requestDto) {

                try {
                        log.info("Generating pre-signed URL for file: {}, folder: {}",
                                        requestDto.getFileName(),
                                        requestDto.getFolder() != null ? requestDto.getFolder() : "root");

                        // Generate the pre-signed URL
                        Map<String, String> presignedData = s3Service.generatePresignedUrl(
                                        requestDto.getFileName(),
                                        requestDto.getContentType(),
                                        requestDto.getFolder(),
                                        requestDto.getExpiresIn(),
                                        requestDto.getContentLength());

                        // Create the response
                        PresignedUrlResponseDto response = PresignedUrlResponseDto.builder()
                                        .url(presignedData.get("url"))
                                        .fileKey(presignedData.get("fileKey"))
                                        .downloadUrl(presignedData.get("downloadUrl"))
                                        .expiresIn(Integer.parseInt(presignedData.get("expiresIn")))
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("Error generating pre-signed URL", e);

                        PresignedUrlResponseDto response = PresignedUrlResponseDto.builder()
                                        .error("Failed to generate pre-signed URL: " + e.getMessage())
                                        .build();

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
        }

        @PostMapping("/download-url/{key}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
        @Operation(summary = "Generate a download URL for an S3 object", description = "Generates a pre-signed download URL for an existing file in S3 with customizable options", responses = {
                        @ApiResponse(responseCode = "200", description = "Download URL generated successfully", content = @Content(schema = @Schema(implementation = DownloadUrlResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "File not found"),
                        @ApiResponse(responseCode = "500", description = "Server error")
        })
        public ResponseEntity<DownloadUrlResponseDto> generateDownloadUrl(
                        @PathVariable("key") String key,
                        @RequestBody(required = false) DownloadUrlRequestDto requestDto) {

                try {
                        log.info("Generating download URL for file key: {}", key);

                        // Use request parameters or defaults
                        Integer expiresIn = requestDto != null && requestDto.getExpiresIn() != null
                                        ? requestDto.getExpiresIn()
                                        : 3600; // Default 1 hour

                        String fileName = requestDto != null ? requestDto.getFileName() : null;
                        Boolean inline = requestDto != null ? requestDto.getInline() : false;

                        // Generate the download URL with all metadata
                        Map<String, Object> downloadData = s3Service.generatePresignedDownloadUrl(
                                        key, expiresIn, fileName, inline);

                        // Create response from the returned data
                        DownloadUrlResponseDto response = new DownloadUrlResponseDto(
                                        (String) downloadData.get("downloadUrl"));
                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("Error generating download URL", e);

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new DownloadUrlResponseDto(
                                                        "Failed to generate download URL: " + e.getMessage()));
                }
        }
}