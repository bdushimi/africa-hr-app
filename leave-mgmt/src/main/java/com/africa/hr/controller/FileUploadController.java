package com.africa.hr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.africa.hr.dto.FileUploadResponseDto;
import com.africa.hr.service.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "API for uploading files to S3")
public class FileUploadController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file to S3", description = "Uploads a file to AWS S3 and returns a downloadable URL", responses = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully", content = @Content(schema = @Schema(implementation = FileUploadResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or request"),
            @ApiResponse(responseCode = "500", description = "Server error during upload")
    })
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        FileUploadResponseDto.builder()
                                .message("File is empty")
                                .build());
            }

            String downloadUrl = s3Service.uploadFile(file);

            FileUploadResponseDto response = FileUploadResponseDto.builder()
                    .fileName(file.getOriginalFilename())
                    .downloadUrl(downloadUrl)
                    .message("File uploaded successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading file to S3", e);

            FileUploadResponseDto response = FileUploadResponseDto.builder()
                    .message("Failed to upload file: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}