/**
 * Response from a successful S3 upload
 */
export interface S3UploadResponse {
  /**
   * The public URL for accessing the uploaded file
   */
  fileUrl: string;
  
  /**
   * The S3 object key (file path within the bucket)
   */
  key: string;
}

/**
 * Response from the backend presigned URL endpoint
 * Matching the actual response from your backend
 */
export interface PresignedUrlResponse {
  /**
   * The presigned URL for uploading directly to S3
   */
  url: string;
  
  /**
   * The S3 object key that will be used
   */
  fileKey: string;
  
  /**
   * The public URL that will be accessible after upload
   */
  downloadUrl: string;
  
  /**
   * Expiration time in seconds
   */
  expiresIn: number;
  
  /**
   * Error message, if any
   */
  error: string | null;
}

/**
 * Response from the backend download URL endpoint
 * Matching the actual response from your backend
 */
export interface DownloadUrlResponse {
  /**
   * The URL that can be used to download the file
   */
  downloadUrl: string;
  
  /**
   * The filename of the original file
   */
  fileName: string;
  
  /**
   * The content type of the file
   */
  contentType: string;
  
  /**
   * When the URL expires (ISO timestamp)
   */
  expiresAt: string;
  
  /**
   * Error message, if any
   */
  error: string | null;
}

/**
 * File validation result
 */
export interface FileValidationResult {
  /**
   * Whether the file is valid
   */
  valid: boolean;
  
  /**
   * Error message if the file is invalid
   */
  message?: string;
} 