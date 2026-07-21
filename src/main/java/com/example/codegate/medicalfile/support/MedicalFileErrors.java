package com.example.codegate.medicalfile.support;

import com.example.codegate.global.BusinessException;
import org.springframework.http.HttpStatus;

public final class MedicalFileErrors {

    private MedicalFileErrors() {
    }

    public static BusinessException fileRequired() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED",
                "업로드할 파일이 필요합니다.");
    }

    public static BusinessException fileTooLarge(long maxFileSizeBytes) {
        return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "파일 크기는 최대 " + (maxFileSizeBytes / 1024 / 1024) + "MB까지 업로드할 수 있습니다.");
    }

    public static BusinessException unsupportedFileType(String extension) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
                "지원하지 않는 파일 형식입니다: " + extension);
    }

    public static BusinessException invalidFileName() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FILE_NAME",
                "파일명이 올바르지 않습니다.");
    }

    public static BusinessException fileSaveFailed() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_SAVE_FAILED",
                "파일 저장에 실패했습니다.");
    }

    public static BusinessException medicalFileNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "MEDICAL_FILE_NOT_FOUND",
                "업로드 파일을 찾을 수 없습니다.");
    }

    public static BusinessException ocrResultNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "OCR_RESULT_NOT_FOUND",
                "OCR 결과를 찾을 수 없습니다.");
    }
}
