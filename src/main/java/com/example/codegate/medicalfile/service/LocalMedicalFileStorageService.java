package com.example.codegate.medicalfile.service;

import com.example.codegate.config.MedicalFileUploadProperties;
import com.example.codegate.medicalfile.entity.MedicalFileType;
import com.example.codegate.medicalfile.support.MedicalFileErrors;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalMedicalFileStorageService {

    private static final Set<String> CHECKUP_EXTENSIONS = Set.of("pdf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "dcm", "pdf", "mp4");

    private final Path rootDir;
    private final long maxFileSizeBytes;

    public LocalMedicalFileStorageService(MedicalFileUploadProperties properties) {
        this.rootDir = Path.of(properties.rootDir()).toAbsolutePath().normalize();
        this.maxFileSizeBytes = properties.maxFileSizeBytes();
    }

    public StoredMedicalFile store(UserAccount patient, MedicalFileType type, MultipartFile file) {
        validate(type, file);

        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;

        LocalDate today = LocalDate.now();
        Path relativeDir = Path.of(
                "medical-files",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                "user-" + patient.getId()
        );
        Path targetDir = rootDir.resolve(relativeDir).normalize();
        ensureInsideRoot(targetDir);

        Path target = targetDir.resolve(storedFileName).normalize();
        ensureInsideRoot(target);

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw MedicalFileErrors.fileSaveFailed();
        }

        return new StoredMedicalFile(
                originalFileName,
                storedFileName,
                file.getContentType(),
                file.getSize(),
                rootDir.relativize(target).toString().replace('\\', '/')
        );
    }

    public void deleteQuietly(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path target = rootDir.resolve(storagePath).normalize();
        if (!target.startsWith(rootDir)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    public Path resolveForRead(String storagePath) {
        Path target = rootDir.resolve(storagePath).normalize();
        if (!target.startsWith(rootDir)) {
            throw MedicalFileErrors.invalidFileName();
        }
        return target;
    }

    private void validate(MedicalFileType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw MedicalFileErrors.fileRequired();
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw MedicalFileErrors.fileTooLarge(maxFileSizeBytes);
        }

        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        Set<String> allowedExtensions = type == MedicalFileType.CHECKUP_RESULT
                ? CHECKUP_EXTENSIONS
                : IMAGE_EXTENSIONS;
        if (!allowedExtensions.contains(extension)) {
            throw MedicalFileErrors.unsupportedFileType(extension);
        }
    }

    private String cleanOriginalFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName).trim();
        if (cleaned.isBlank() || cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw MedicalFileErrors.invalidFileName();
        }
        return cleaned;
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            throw MedicalFileErrors.unsupportedFileType("");
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(rootDir)) {
            throw MedicalFileErrors.invalidFileName();
        }
    }
}
