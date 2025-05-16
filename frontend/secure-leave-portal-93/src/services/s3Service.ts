import axios from 'axios';
import api from './api';
import { PresignedUrlResponse, DownloadUrlResponse, S3UploadResponse } from '@/types/s3Types';

interface UploadProgressCallback {
  (progress: number): void;
}

class S3Service {
  private static instance: S3Service;

  private constructor() {}

  public static getInstance(): S3Service {
    if (!S3Service.instance) {
      S3Service.instance = new S3Service();
    }
    return S3Service.instance;
  }

  /**
   * Get pre-signed URL from backend for direct S3 upload
   * @param fileName - Name of the file to upload
   * @param contentType - MIME type of the file
   */
  public async getPresignedUrl(fileName: string, contentType: string): Promise<PresignedUrlResponse> {
    try {
      const response = await api.post('/s3/presigned-url', {
        fileName,
        contentType
      });
      return response.data;
    } catch (error) {
      console.error('Error getting presigned URL:', error);
      throw new Error('Failed to get upload URL');
    }
  }

  /**
   * Upload a file directly to S3 using a presigned URL
   * @param file - The file to upload
   * @param onProgress - Optional callback for upload progress
   * @returns The URL of the uploaded file and the S3 key
   */
  public async uploadFile(
    file: File, 
    onProgress?: UploadProgressCallback
  ): Promise<S3UploadResponse> {
    try {
      // Generate a unique file key with timestamp and random string
      const timestamp = new Date().getTime();
      const randomString = Math.random().toString(36).substring(2, 15);
      const safeFileName = file.name.replace(/[^a-zA-Z0-9.-]/g, '_');
      const uniqueFileName = `${timestamp}-${randomString}-${safeFileName}`;
      
      // Get a presigned URL from our backend
      const { url, fileKey, downloadUrl } = await this.getPresignedUrl(
        uniqueFileName,
        file.type
      );

      // Upload to S3 directly using the presigned URL
      await axios.put(url, file, {
        headers: {
          'Content-Type': file.type,
          'Content-Disposition': `attachment; filename="${uniqueFileName}"`,
          // Add any other headers that might be needed
          'x-amz-acl': 'public-read' // Optional, if you want the file to be publicly readable
        },
        // Important for CORS preflight requests
        withCredentials: false,
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(percentCompleted);
          }
        }
      });

      return {
        fileUrl: downloadUrl,
        key: fileKey
      };
    } catch (error: any) {
      // Enhanced error logging for troubleshooting
      console.error("Error uploading file to S3:", error);
      
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.error("S3 Error Response:", {
          data: error.response.data,
          status: error.response.status,
          headers: error.response.headers
        });
      } else if (error.request) {
        // The request was made but no response was received
        // `error.request` is an instance of XMLHttpRequest
        console.error("S3 No Response Error:", error.request);
        
        // CORS specific error handling
        if (error.message && error.message.includes("Network Error")) {
          throw new Error("CORS error: Your browser blocked the upload to S3. Please contact support to ensure the S3 bucket has the proper CORS configuration.");
        }
      } else {
        // Something happened in setting up the request that triggered an Error
        console.error("S3 Request Setup Error:", error.message);
      }
      
      throw new Error(`Failed to upload file: ${error.message || 'Unknown error'}`);
    }
  }
  
  /**
   * Get a downloadable URL for a file in S3
   * @param key - The S3 object key
   * @returns The download URL response with URL and metadata
   */
  public async getDownloadUrl(key: string): Promise<DownloadUrlResponse> {
    try {
      const response = await api.get(`/s3/download-url/${key}`);
      return response.data;
    } catch (error) {
      console.error('Error getting download URL:', error);
      throw new Error('Failed to get download URL');
    }
  }

  /**
   * Upload a file through the backend as a fallback method (no CORS issues)
   * Use this method if direct S3 upload fails due to CORS
   * @param file - The file to upload
   * @param onProgress - Optional callback for upload progress
   */
  public async uploadThroughBackend(
    file: File,
    onProgress?: UploadProgressCallback
  ): Promise<S3UploadResponse> {
    try {
      // Create a FormData object to send the file
      const formData = new FormData();
      formData.append('file', file);
      
      // Upload through backend endpoint
      const response = await api.post('/s3/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(percentCompleted);
          }
        }
      });
      
      return {
        fileUrl: response.data.downloadUrl || response.data.fileUrl,
        key: response.data.fileKey || response.data.key
      };
    } catch (error: any) {
      console.error('Error uploading file through backend:', error);
      throw new Error(`Failed to upload file through backend: ${error.message || 'Unknown error'}`);
    }
  }

  /**
   * Smart upload method that tries direct S3 upload first, then falls back to backend
   * @param file - The file to upload 
   * @param onProgress - Optional callback for upload progress
   */
  public async smartUpload(
    file: File,
    onProgress?: UploadProgressCallback
  ): Promise<S3UploadResponse> {
    try {
      // Try direct S3 upload first
      return await this.uploadFile(file, onProgress);
    } catch (error: any) {
      // If error contains CORS or Network Error, try backend upload
      if (
        error.message.includes('CORS') || 
        error.message.includes('Network Error') ||
        error.code === 'ERR_NETWORK'
      ) {
        console.log('Direct S3 upload failed due to CORS, trying backend upload...');
        return await this.uploadThroughBackend(file, onProgress);
      }
      // Otherwise rethrow the original error
      throw error;
    }
  }
}

export default S3Service.getInstance(); 