package com.referralcrm.service;

import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.referralcrm.api.ApiException;

@Service
public class ResumeParserService {
    private static final int MAX_TEXT_CHARS=500_000;
    private static final Pattern MULTI_SPACE=Pattern.compile("[\\t\\x0B\\f\\r ]+");
    private final int maxPages;
    public ResumeParserService(@Value("${app.resume-max-pages:200}") int maxPages) { this.maxPages=Math.max(1,Math.min(1000,maxPages)); }
    public String extract(byte[] pdfBytes) {
        if(pdfBytes==null||pdfBytes.length<5||pdfBytes[0]!='%'||pdfBytes[1]!='P'||pdfBytes[2]!='D'||pdfBytes[3]!='F'||pdfBytes[4]!='-') throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Upload a valid PDF document");
        try(PDDocument document=Loader.loadPDF(pdfBytes)) {
            if(document.isEncrypted()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Password-protected PDFs are not supported");
            if(document.getNumberOfPages()>maxPages) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"PDF exceeds the supported page limit");
            PDFTextStripper stripper=new PDFTextStripper();
            String raw=stripper.getText(document);
            String normalized=raw.replace("\u0000","").replace("\r\n","\n").replace('\r','\n');
            StringBuilder text=new StringBuilder(Math.min(normalized.length(),MAX_TEXT_CHARS));
            for(String line:normalized.split("\\n",-1)) {
                String tidy=MULTI_SPACE.matcher(line).replaceAll(" ").strip();
                if(!tidy.isEmpty()) text.append(tidy).append('\n');
                else if(text.length()>0&&text.charAt(text.length()-1)=='\n'&&(text.length()<2||text.charAt(text.length()-2)!='\n')) text.append('\n');
                if(text.length()>=MAX_TEXT_CHARS) break;
            }
            String result=text.toString().strip();
            if(result.isBlank()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"No selectable text was found. Scanned/image-only PDFs are not supported yet.");
            return result.length()>MAX_TEXT_CHARS?result.substring(0,MAX_TEXT_CHARS):result;
        } catch(ApiException e) { throw e; }
        catch(InvalidPasswordException e) { throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Password-protected PDFs are not supported"); }
        catch(IOException|RuntimeException e) { throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Could not read this PDF. Check that it is valid and not damaged."); }
    }
}
