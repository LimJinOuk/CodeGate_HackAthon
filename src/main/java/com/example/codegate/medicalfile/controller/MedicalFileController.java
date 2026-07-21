package com.example.codegate.medicalfile.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.medicalfile.dto.MedicalFileOcrResultResponse;
import com.example.codegate.medicalfile.dto.MedicalFileUploadResponse;
import com.example.codegate.medicalfile.entity.MedicalFileType;
import com.example.codegate.medicalfile.service.MedicalFileOcrResultService;
import com.example.codegate.medicalfile.service.MedicalFileService;
import com.example.codegate.reservation.support.CallerResolver;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/patient/medical-files")
public class MedicalFileController {

    private final CallerResolver callerResolver;
    private final MedicalFileService medicalFileService;
    private final MedicalFileOcrResultService ocrResultService;

    public MedicalFileController(CallerResolver callerResolver,
                                 MedicalFileService medicalFileService,
                                 MedicalFileOcrResultService ocrResultService) {
        this.callerResolver = callerResolver;
        this.medicalFileService = medicalFileService;
        this.ocrResultService = ocrResultService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicalFileUploadResponse> upload(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam MedicalFileType type,
            @RequestParam MultipartFile file) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(medicalFileService.upload(patient, type, file));
    }

    @GetMapping("/{medicalFileId}/ocr-result")
    public ApiResponse<MedicalFileOcrResultResponse> ocrResult(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long medicalFileId) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(ocrResultService.findMine(patient, medicalFileId));
    }
}
