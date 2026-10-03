package com.referralcrm.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ResumeParserServiceTest {
    private final ResumeParserService parser=new ResumeParserService(10);
    @Test void extractsAndNormalizesSelectablePdfText() throws Exception {
        byte[] pdf=pdf("Resume Summary\n\nSkills: Java, Spring Boot, AWS");
        String text=parser.extract(pdf);
        assertTrue(text.contains("Resume Summary")); assertTrue(text.contains("Skills: Java, Spring Boot, AWS"));
    }
    @Test void rejectsNonPdfBytes() { assertThrows(RuntimeException.class,()->parser.extract("not a pdf".getBytes())); }
    private byte[] pdf(String text) throws Exception {
        try(PDDocument doc=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            PDPage page=new PDPage();doc.addPage(page);
            try(PDPageContentStream stream=new PDPageContentStream(doc,page)) {
                stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(50,750);
                for(String line:text.split("\\n")){stream.showText(line);stream.newLineAtOffset(0,-18);}
                stream.endText();
            }
            doc.save(output);return output.toByteArray();
        }
    }
}
