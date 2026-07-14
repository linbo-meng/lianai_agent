package com.yupi.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yupi.yuaiagent.constant.FileConstant;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * PDF生成工具
 */
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        Path targetPath = Path.of(fileDir, fileName);
        Path tempPath = null;
        try {
            FileUtil.mkdir(fileDir);
            tempPath = Files.createTempFile(Path.of(fileDir), "pdf-", ".tmp");

            try (PdfWriter writer = new PdfWriter(tempPath.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 使用内置中文字体
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                document.add(new Paragraph(content));
            }

            if (!isCompletePdf(tempPath)) {
                Files.deleteIfExists(tempPath);
                return "Error generating PDF: incomplete PDF structure (missing %%EOF)";
            }

            moveToTarget(tempPath, targetPath);
            tempPath = null;
            return "PDF generated successfully to: " + targetPath;
        } catch (Exception e) {
            cleanupQuietly(tempPath);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private static void moveToTarget(Path tempPath, Path targetPath) throws IOException {
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isCompletePdf(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < 8) {
            return false;
        }
        String ascii = new String(bytes, StandardCharsets.ISO_8859_1);
        return ascii.startsWith("%PDF-") && ascii.contains("%%EOF");
    }

    private static void cleanupQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup of partial files
        }
    }
}
