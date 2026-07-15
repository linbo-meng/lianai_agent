package com.yupi.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
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

    @Tool(description = "Generate a PDF file with given content. Prefer this tool when user asks for a PDF plan or document. fileName MUST be English letters/numbers/underscore and end with .pdf, e.g. tianjin_qixi_plan.pdf")
    public String generatePDF(
            @ToolParam(description = "ASCII file name ending with .pdf, e.g. tianjin_qixi_plan.pdf") String fileName,
            @ToolParam(description = "Content to be included in the PDF. Prefer plain Chinese/English text, avoid emoji.") String content) {
        String safeName = sanitizeFileName(fileName);
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        Path targetPath = Path.of(fileDir, safeName);
        Path tempPath = null;
        try {
            FileUtil.mkdir(fileDir);
            tempPath = Files.createTempFile(Path.of(fileDir), "pdf-", ".tmp");

            PdfFont font = createChineseFont();
            String safeContent = filterUnsupportedChars(font, content);

            try (PdfWriter writer = new PdfWriter(tempPath.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                document.setFont(font);
                for (String paragraph : safeContent.split("\\r?\\n")) {
                    if (paragraph.isBlank()) {
                        document.add(new Paragraph(" "));
                    } else {
                        document.add(new Paragraph(paragraph).setFont(font));
                    }
                }
            }

            if (!isCompletePdf(tempPath)) {
                Files.deleteIfExists(tempPath);
                return "Error generating PDF: incomplete PDF structure (missing %%EOF)";
            }

            moveToTarget(tempPath, targetPath);
            tempPath = null;
            return "PDF generated successfully to: " + targetPath
                    + " | download: type=pdf&name=" + safeName;
        } catch (Exception e) {
            cleanupQuietly(tempPath);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 优先使用 Windows 可嵌入的 TTF/TTC 中文字体（最稳）。
     * Adobe CID 字体作为兜底。
     */
    private static PdfFont createChineseFont() throws IOException {
        String[] localFonts = {
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simkai.ttf",
                "/System/Library/Fonts/PingFang.ttc,0",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
        };

        for (String fontPath : localFonts) {
            String diskPath = fontPath.contains(",")
                    ? fontPath.substring(0, fontPath.indexOf(','))
                    : fontPath;
            if (!Files.exists(Path.of(diskPath))) {
                continue;
            }
            try {
                return PdfFontFactory.createFont(
                        fontPath,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception ignored) {
                // try next
            }
        }

        // font-asian CID 字体（不嵌入策略更稳妥）
        try {
            return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception ignored) {
            // fall through
        }

        return PdfFontFactory.createFont();
    }

    /**
     * 去掉当前字体无法绘制的字符（emoji/罕见符号），避免 Glyph NPE
     */
    private static String filterUnsupportedChars(PdfFont font, String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(content.length());
        content.codePoints().forEach(cp -> {
            if (cp == '\n' || cp == '\r' || cp == '\t' || cp == ' ') {
                sb.appendCodePoint(cp);
                return;
            }
            // 常见会触发缺字的符号块
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            if (block == Character.UnicodeBlock.EMOTICONS
                    || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
                    || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
                    || block == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
                    || block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
                    || block == Character.UnicodeBlock.DINGBATS
                    || block == Character.UnicodeBlock.VARIATION_SELECTORS) {
                return;
            }
            try {
                if (font.containsGlyph(cp)) {
                    sb.appendCodePoint(cp);
                }
            } catch (Exception e) {
                // 跳过异常字符
            }
        });
        return sb.toString();
    }

    private static String sanitizeFileName(String fileName) {
        String name = fileName == null ? "document.pdf" : fileName.trim();
        name = Path.of(name).getFileName().toString();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        // 仅保留 ASCII，避免 Windows 控制台/下载时中文乱码导致找不到文件
        String ascii = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("_+", "_");
        if (ascii.isBlank() || ascii.equals(".pdf") || ascii.equals("_") || ascii.equals("_.pdf")) {
            ascii = "document.pdf";
        }
        if (!ascii.toLowerCase().endsWith(".pdf")) {
            ascii = ascii + ".pdf";
        }
        return ascii;
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
