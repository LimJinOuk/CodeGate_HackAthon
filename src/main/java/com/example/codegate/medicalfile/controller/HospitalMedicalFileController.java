package com.example.codegate.medicalfile.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.medicalfile.dto.MedicalFileContentResponse;
import com.example.codegate.medicalfile.dto.MedicalFileOcrResultResponse;
import com.example.codegate.medicalfile.dto.MedicalFileResponse;
import com.example.codegate.medicalfile.service.MedicalFileOcrResultService;
import com.example.codegate.medicalfile.service.MedicalFileService;
import com.example.codegate.reservation.support.CallerResolver;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({
        "/api/v1/hospital/reservations/{reservationId}/medical-files",
        "/api/v1/hospitals/me/reservations/{reservationId}/medical-files"
})
public class HospitalMedicalFileController {

    private final CallerResolver callerResolver;
    private final MedicalFileService medicalFileService;
    private final MedicalFileOcrResultService ocrResultService;

    public HospitalMedicalFileController(CallerResolver callerResolver,
                                         MedicalFileService medicalFileService,
                                         MedicalFileOcrResultService ocrResultService) {
        this.callerResolver = callerResolver;
        this.medicalFileService = medicalFileService;
        this.ocrResultService = ocrResultService;
    }

    @GetMapping
    public ApiResponse<List<MedicalFileResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(medicalFileService.findForHospitalReservation(hospital, reservationId));
    }

    @GetMapping("/{medicalFileId}/ocr-result")
    public ApiResponse<MedicalFileOcrResultResponse> ocrResult(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId,
            @PathVariable Long medicalFileId) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(ocrResultService.findForHospitalReservation(hospital, reservationId, medicalFileId));
    }

    @GetMapping("/{medicalFileId}/content")
    public ResponseEntity<?> content(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId,
            @PathVariable Long medicalFileId) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        MedicalFileContentResponse content = medicalFileService.findContentForHospitalReservation(
                hospital, reservationId, medicalFileId);

        return ResponseEntity.ok()
                .contentType(mediaType(content.contentType()))
                .contentLength(content.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content.resource());
    }

    private MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
