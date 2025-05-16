import axios from 'axios';
import api from './api';

export interface PresignUploadResponse {
  uploadUrl: string;
  blobUrl: string;
}

export interface RegisterDocumentResponse {
  // Define fields if backend returns any, otherwise void
}

export interface DownloadUrlResponse {
  downloadUrl: string;
}

class AzureBlobService {
  async getPresignedUploadUrl(filename: string, mimeType: string): Promise<PresignUploadResponse> {
    const response = await api.post('/documents/presign-upload', {
      filename,
      mimeType,
    });
    return response.data;
  }

  async uploadFileToAzure(uploadUrl: string, file: File, onProgress?: (progress: number) => void): Promise<void> {
    await axios.put(uploadUrl, file, {
      headers: {
        'x-ms-blob-type': 'BlockBlob',
        'Content-Type': file.type,
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percentCompleted);
        }
      },
    });
  }

  async registerDocument(leaveRequestId: number, name: string, blobUrl: string): Promise<RegisterDocumentResponse> {
    const response = await api.post(`/leave-requests/${leaveRequestId}/documents`, {
      name,
      blobUrl,
    });
    return response.data;
  }

  async getDownloadUrl(blobUrl: string): Promise<DownloadUrlResponse> {
    const response = await api.get('/documents/download-url', {
      params: { blobUrl },
    });
    return response.data;
  }

  async setDocumentVisibility(leaveRequestId: number, documentId: number, visible: boolean): Promise<void> {
    await api.patch(`/leave-requests/${leaveRequestId}/documents/${documentId}/visibility`, {
      visible,
    });
  }
}

export default new AzureBlobService(); 