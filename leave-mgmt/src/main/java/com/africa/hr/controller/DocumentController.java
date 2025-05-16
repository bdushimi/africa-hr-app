package com.africa.hr.controller;

import com.africa.hr.dto.*;
import com.africa.hr.model.Document;
import com.africa.hr.model.LeaveRequest;
import com.africa.hr.repository.DocumentRepository;
import com.africa.hr.repository.LeaveRequestRepository;
import com.africa.hr.service.AzureBlobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "APIs for uploading, downloading, and managing document visibility for leave requests.")
public class DocumentController {
    private final AzureBlobService azureBlobService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final DocumentRepository documentRepository;

    @Operation(summary = "Generate a presigned upload URL for Azure Blob Storage", description = "Returns a temporary upload URL and blob URL for direct file upload to Azure Blob Storage.")
    @PostMapping("/presign-upload")
    public ResponseEntity<PresignUploadResponseDto> presignUpload(@RequestBody @Valid PresignUploadRequestDto request) {
        // Generate unique blob name (e.g., UUID + original filename)
        String blobName = java.util.UUID.randomUUID() + "-" + request.getFilename();
        String uploadUrl = azureBlobService.generateUploadSasUrl(blobName, request.getMimeType());
        String blobUrl = azureBlobService.buildBlobUrl(blobName);
        return ResponseEntity.ok(new PresignUploadResponseDto(uploadUrl, blobUrl));
    }

    @Operation(summary = "Register document metadata to a leave request", description = "Stores document metadata (name, blobUrl) and links it to a leave request.")
    @PostMapping("/leave-requests/{leaveRequestId}/documents")
    public ResponseEntity<Void> addDocumentToLeaveRequest(
            @Parameter(description = "ID of the leave request") @PathVariable Long leaveRequestId,
            @RequestBody @Valid AddDocumentRequestDto request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new IllegalArgumentException("LeaveRequest not found: " + leaveRequestId));
        Document document = new Document();
        document.setName(request.getName());
        document.setBlobUrl(request.getBlobUrl());
        document.setVisible(true);
        document.setUploadedAt(LocalDateTime.now());
        document.setLeaveRequest(leaveRequest);
        documentRepository.save(document);
        return ResponseEntity.created(URI.create(request.getBlobUrl())).build();
    }

    @Operation(summary = "Generate a presigned download URL for a document", description = "Returns a temporary download URL for a document in Azure Blob Storage.")
    @GetMapping("/download-url")
    public ResponseEntity<DownloadUrlResponseDto> getDownloadUrl(
            @Parameter(description = "Blob URL of the document") @RequestParam(required = false) String blobUrl) {
        if (blobUrl == null) {
            System.out.println("ERROR: blobUrl param is missing");
            return ResponseEntity.badRequest().body(new DownloadUrlResponseDto("ERROR: blobUrl param is missing"));
        }
        String downloadUrl = azureBlobService.generateDownloadSasUrl(blobUrl);
        return ResponseEntity.ok(new DownloadUrlResponseDto(downloadUrl));
    }

    @Operation(summary = "Set document visibility", description = "Toggles the visibility status of a document linked to a leave request.")
    @PatchMapping("/leave-requests/{leaveRequestId}/documents/{documentId}/visibility")
    public ResponseEntity<Void> setDocumentVisibility(
            @Parameter(description = "ID of the leave request") @PathVariable Long leaveRequestId,
            @Parameter(description = "ID of the document") @PathVariable Long documentId,
            @RequestBody @Valid SetVisibilityRequestDto request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (!document.getLeaveRequest().getId().equals(leaveRequestId)) {
            return ResponseEntity.badRequest().build();
        }
        document.setVisible(request.getVisible());
        documentRepository.save(document);
        return ResponseEntity.noContent().build();
    }
}