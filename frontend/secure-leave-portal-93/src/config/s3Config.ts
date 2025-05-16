// S3 Configuration
export const S3_CONFIG = {
  // Maximum file size in bytes (20MB)
  MAX_FILE_SIZE: 20 * 1024 * 1024,
  
  // Allowed file types for upload
  ALLOWED_FILE_TYPES: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ],
  
  // File extension to MIME type mapping
  FILE_EXTENSIONS: {
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    pdf: 'application/pdf',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  }
};

/**
 * Validates if a file is valid for upload
 * @param file The file to validate
 * @returns An object with validation result and error message
 */
export const validateFile = (file: File): { valid: boolean; message?: string } => {
  // Check file size
  if (file.size > S3_CONFIG.MAX_FILE_SIZE) {
    return {
      valid: false,
      message: `File is too large. Maximum size is ${S3_CONFIG.MAX_FILE_SIZE / (1024 * 1024)}MB.`
    };
  }

  // Check file type
  if (!S3_CONFIG.ALLOWED_FILE_TYPES.includes(file.type)) {
    return {
      valid: false,
      message: 'File type not supported. Please upload a PDF, JPG, PNG, or document file.'
    };
  }

  return { valid: true };
};

export default S3_CONFIG; 