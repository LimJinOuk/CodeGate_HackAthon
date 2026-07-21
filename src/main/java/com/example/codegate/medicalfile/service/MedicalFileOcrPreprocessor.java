package com.example.codegate.medicalfile.service;

import com.example.codegate.config.OcrProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
public class MedicalFileOcrPreprocessor {

    private static final int MAX_RAW_JPEG_BYTES = 7 * 1024 * 1024;

    private final OcrProperties properties;

    public MedicalFileOcrPreprocessor(OcrProperties properties) {
        this.properties = properties;
    }

    public List<OcrImagePayload> prepareImages(Path filePath, String originalFileName) {
        String extension = extensionOf(originalFileName);
        try {
            if ("pdf".equals(extension)) {
                return renderPdfPages(filePath);
            }
            if ("jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension)) {
                return List.of(prepareSingleImage(filePath, "image"));
            }
            throw new IllegalArgumentException("OCR을 지원하지 않는 파일 형식입니다: " + extension);
        } catch (IOException exception) {
            throw new IllegalStateException("OCR 전처리에 실패했습니다.", exception);
        }
    }

    private List<OcrImagePayload> renderPdfPages(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount > properties.maxPdfPages()) {
                throw new IllegalArgumentException("PDF는 최대 " + properties.maxPdfPages() + "페이지까지만 OCR 처리할 수 있습니다.");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            List<OcrImagePayload> images = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, properties.pdfRenderDpi(), ImageType.RGB);
                images.add(toPayload(pageImage, "page-" + (pageIndex + 1)));
            }
            return images;
        }
    }

    private OcrImagePayload prepareSingleImage(Path filePath, String label) throws IOException {
        BufferedImage image = ImageIO.read(filePath.toFile());
        if (image == null) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
        }
        return toPayload(image, label);
    }

    private OcrImagePayload toPayload(BufferedImage source, String label) throws IOException {
        BufferedImage resized = resizeIfNeeded(source, properties.maxImageLongEdge());
        byte[] jpegBytes = writeJpegUnderLimit(resized);
        return new OcrImagePayload(
                label,
                "image/jpeg",
                Base64.getEncoder().encodeToString(jpegBytes)
        );
    }

    private byte[] writeJpegUnderLimit(BufferedImage image) throws IOException {
        BufferedImage current = image;
        float quality = properties.jpegQuality();

        for (int attempt = 0; attempt < 8; attempt++) {
            byte[] bytes = writeJpeg(current, quality);
            if (bytes.length <= MAX_RAW_JPEG_BYTES) {
                return bytes;
            }
            quality = Math.max(0.55f, quality - 0.08f);
            current = scale(current, Math.max(1, Math.round(current.getWidth() * 0.85f)),
                    Math.max(1, Math.round(current.getHeight() * 0.85f)));
        }

        return writeJpeg(current, 0.55f);
    }

    private BufferedImage resizeIfNeeded(BufferedImage image, int maxLongEdge) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longEdge = Math.max(width, height);
        if (longEdge <= maxLongEdge) {
            return toRgb(image);
        }

        double ratio = (double) maxLongEdge / longEdge;
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));
        return scale(image, targetWidth, targetHeight);
    }

    private BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer를 찾을 수 없습니다.");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.min(1.0f, Math.max(0.1f, quality)));
            writer.write(null, new IIOImage(toRgb(image), null, null), param);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String extensionOf(String fileName) {
        int lastDotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
