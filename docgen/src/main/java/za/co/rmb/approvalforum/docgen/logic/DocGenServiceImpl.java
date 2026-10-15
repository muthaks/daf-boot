package za.co.rmb.approvalforum.docgen.logic;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import za.co.rmb.approvalforum.docgen.controller.DocGenController;
import za.co.rmb.approvalforum.docgen.model.AshburtonReportOutputModel;
import za.co.rmb.approvalforum.docgen.model.PingModel;
import za.co.rmb.approvalforum.docgen.model.ReportOutputModel;

import java.io.*;
import java.nio.file.FileSystems;

import static com.itextpdf.text.pdf.BaseFont.EMBEDDED;
import static com.itextpdf.text.pdf.BaseFont.IDENTITY_H;
import static org.thymeleaf.templatemode.TemplateMode.HTML;

@Component
public class DocGenServiceImpl implements DocGenService {

    @Value("${docgen.debug.logging}")
    private boolean debugLogging;

    @Value("classpath:/calibri.ttf")
    Resource CALIBRI_FONT;

    @Value("classpath:/RMB_Horizontal_Identity_RGB_Black.png")
    Resource RMB_LOGO;

    @Value("classpath:/FNB_Horizontal_Identity_RGB.png")
    Resource FNB_LOGO;

    @Value("classpath:/style.css")
    Resource STYLE_CSS;

    private static final String UTF_8 = "UTF-8";

    private static final Logger logger = LoggerFactory.getLogger(DocGenServiceImpl.class);
    @Override
    public ByteSource generateReport(ReportOutputModel reportOutputModel) {
        byte[] result = null;
        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
        // are HTML-based files located under "src/test/resources". Beside
        // of the main HTML file, we also have partials like a footer or
        // a header. We can re-use those partials in different documents.
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("model", reportOutputModel);

        // Flying Saucer needs XHTML - not just normal HTML. To make our life
        // easy, we use JTidy to convert the rendered Thymeleaf template to
        // XHTML. Note that this might not work for very complicated HTML. But
        // it's good enough for a simple letter.
        String renderedHtmlContent = templateEngine.process("template", context);
        String xHtml = "";
        try {
            xHtml = convertToXhtml(renderedHtmlContent);
            if (debugLogging) {
                System.out.println(xHtml);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ITextRenderer renderer = new ITextRenderer();
        try {
            renderer.getFontResolver().addFont("calibri.ttf", IDENTITY_H, EMBEDDED);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

        // FlyingSaucer has a working directory. If you run this test, the working
        // directory
        // will be the root folder of your project. However, all files (HTML, CSS, etc.)
        // are
        // located under "/src/test/resources". So we want to use this folder as the
        // working
        // directory.
        String baseUrl;
        try {
            setpReportTemplate();
            baseUrl = FileSystems.getDefault().getPath(System.getenv("DAF_RESOURCES")).toUri().toURL().toString();
            renderer.setDocumentFromString(xHtml, baseUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        renderer.layout();

        // And finally, we create the PDF:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            renderer.createPDF(outputStream);            
            outputStream.close();
            
            // result = addWatermark(outputStream, "COMPLETED", 55).toByteArray();

            result = outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
        
        return ByteSource.wrap(result);
    }

    //ashburton
    @Override
    public ByteSource generateASBReport(AshburtonReportOutputModel reportOutputModel) {
        logger.info("generateASBReport(START)");
        byte[] result = null;
        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
        // are HTML-based files located under "src/test/resources". Beside
        // of the main HTML file, we also have partials like a footer or
        // a header. We can re-use those partials in different documents.
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("model", reportOutputModel);

        // Flying Saucer needs XHTML - not just normal HTML. To make our life
        // easy, we use JTidy to convert the rendered Thymeleaf template to
        // XHTML. Note that this might not work for very complicated HTML. But
        // it's good enough for a simple letter.
        String renderedHtmlContent = templateEngine.process("template_ashburton", context);
        String xHtml = "";
        try {
            xHtml = convertToXhtml(renderedHtmlContent);
            if (debugLogging) {
                System.out.println(xHtml);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ITextRenderer renderer = new ITextRenderer();
        try {
            renderer.getFontResolver().addFont("calibri.ttf", IDENTITY_H, EMBEDDED);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

        // FlyingSaucer has a working directory. If you run this test, the working
        // directory
        // will be the root folder of your project. However, all files (HTML, CSS, etc.)
        // are
        // located under "/src/test/resources". So we want to use this folder as the
        // working
        // directory.
        String baseUrl;
        try {
            setpReportTemplate();
            baseUrl = FileSystems.getDefault().getPath(System.getenv("DAF_RESOURCES")).toUri().toURL().toString();
            renderer.setDocumentFromString(xHtml, baseUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        renderer.layout();

        // And finally, we create the PDF:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            renderer.createPDF(outputStream);
            outputStream.close();

            // result = addWatermark(outputStream, "COMPLETED", 55).toByteArray();

            result = outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
        logger.info("generateASBReport(END)");
        return ByteSource.wrap(result);
    }

    @Override
    public ByteSource generateBroaderAfricaReport(ReportOutputModel reportOutputModel) {
        byte[] result = null;
        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
        // are HTML-based files located under "src/test/resources". Beside
        // of the main HTML file, we also have partials like a footer or
        // a header. We can re-use those partials in different documents.
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("model", reportOutputModel);

        // Flying Saucer needs XHTML - not just normal HTML. To make our life
        // easy, we use JTidy to convert the rendered Thymeleaf template to
        // XHTML. Note that this might not work for very complicated HTML. But
        // it's good enough for a simple letter.
        String renderedHtmlContent = templateEngine.process("template", context);
        String xHtml = "";
        try {
            xHtml = convertToXhtml(renderedHtmlContent);
            if (debugLogging) {
                System.out.println(xHtml);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ITextRenderer renderer = new ITextRenderer();
        try {
            renderer.getFontResolver().addFont("calibri.ttf", IDENTITY_H, EMBEDDED);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

        // FlyingSaucer has a working directory. If you run this test, the working
        // directory
        // will be the root folder of your project. However, all files (HTML, CSS, etc.)
        // are
        // located under "/src/test/resources". So we want to use this folder as the
        // working
        // directory.
        String baseUrl;
        try {
            setpBroaderAfricaReportTemplate();
            baseUrl = FileSystems.getDefault().getPath(System.getenv("DAF_RESOURCES")).toUri().toURL().toString();
            renderer.setDocumentFromString(xHtml, baseUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        renderer.layout();

        // And finally, we create the PDF:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            renderer.createPDF(outputStream);
            outputStream.close();

            // result = addWatermark(outputStream, "COMPLETED", 55).toByteArray();

            result = outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

        return ByteSource.wrap(result);
    }

    private void setpBroaderAfricaReportTemplate() throws Exception {
        String fontsPath = System.getenv("DAF_RESOURCES");

        if (fontsPath == null) {
            throw new Exception("Please set DAF_RESOURCES environment variable.");
        }

        String calibriPath = fontsPath + "/calibri.ttf";
        String stylePath = fontsPath + "/style.css";
        String logoPath = fontsPath + "/RMB_Horizontal_Identity_RGB_Black.png";

        File reportTemplatePathFile = new File(fontsPath);
        File calibriPathFile = new File(calibriPath);
        File stylePathFile = new File(stylePath);
        File logoPathFile = new File(logoPath);

        if (!reportTemplatePathFile.exists()) {
            reportTemplatePathFile.mkdir();
        }

        if (!calibriPathFile.exists()) {
            try {
                OutputStream calibriOut = new FileOutputStream(calibriPathFile);
                calibriOut.write(ByteStreams.toByteArray(CALIBRI_FONT.getInputStream()));
                calibriOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!stylePathFile.exists()) {
            try {
                OutputStream styleOut = new FileOutputStream(stylePathFile);
                styleOut.write(ByteStreams.toByteArray(STYLE_CSS.getInputStream()));
                styleOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!logoPathFile.exists()) {
            try {
                OutputStream logoOut = new FileOutputStream(logoPathFile);
                //logo path as to change
                logoOut.write(ByteStreams.toByteArray(FNB_LOGO.getInputStream()));
                logoOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PingModel ping() {
        return new PingModel(true);
    }

    private ByteArrayOutputStream addWatermark(ByteArrayOutputStream src, String text, int rotation) {
        PdfReader reader = null;
        PdfStamper pdfStamper = null;
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        try {
            reader = new PdfReader(src.toByteArray());
            pdfStamper = new PdfStamper(reader, target);

            setupFonts();

            addWatermark(pdfStamper, text, rotation);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pdfStamper != null) {
                try {
                    pdfStamper.close();
                } catch (DocumentException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return target;
    }

    private void setupFonts() throws Exception {
        String fontsPath = System.getenv("DAF_RESOURCES");

        if (fontsPath == null) {
            throw new Exception("Please set DAF_RESOURCES environment variable.");
        }

        String calibriPath = fontsPath + "/calibri.ttf";

        File fontsPathFile = new File(fontsPath);
        File calibriPathFile = new File(calibriPath);

        if (!fontsPathFile.exists()) {
            fontsPathFile.mkdir();
        }

        if (calibriPathFile.exists()) {
            FontFactory.register(calibriPath, "Calibri");
        } else {
            try {
                OutputStream calibriOut = new FileOutputStream(calibriPathFile);
                calibriOut.write(ByteStreams.toByteArray(CALIBRI_FONT.getInputStream()));
                calibriOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            FontFactory.register(calibriPath, "Calibri");
        }
    }

    private void setpReportTemplate() throws Exception {
        String fontsPath = System.getenv("DAF_RESOURCES");
        
        if (fontsPath == null) {
            throw new Exception("Please set DAF_RESOURCES environment variable.");
        }

        String calibriPath = fontsPath + "/calibri.ttf";
        String stylePath = fontsPath + "/style.css";
        String logoPath = fontsPath + "/RMB_Horizontal_Identity_RGB_Black.png";

        File reportTemplatePathFile = new File(fontsPath);
        File calibriPathFile = new File(calibriPath);
        File stylePathFile = new File(stylePath);
        File logoPathFile = new File(logoPath);

        if (!reportTemplatePathFile.exists()) {
            reportTemplatePathFile.mkdir();
        }

        if (!calibriPathFile.exists()) {
            try {
                OutputStream calibriOut = new FileOutputStream(calibriPathFile);
                calibriOut.write(ByteStreams.toByteArray(CALIBRI_FONT.getInputStream()));
                calibriOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!stylePathFile.exists()) {
            try {
                OutputStream styleOut = new FileOutputStream(stylePathFile);
                styleOut.write(ByteStreams.toByteArray(STYLE_CSS.getInputStream()));
                styleOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (!logoPathFile.exists()) {
            try {
                OutputStream logoOut = new FileOutputStream(logoPathFile);
                logoOut.write(ByteStreams.toByteArray(RMB_LOGO.getInputStream()));
                logoOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void addWatermark(PdfStamper pdfStamper, String watermark, int rotation){
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.4f);
        gs.setStrokeOpacity(0.4f);

        Font font = FontFactory.getFont("Calibri", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 0.8f, Font.NORMAL,
                BaseColor.BLACK);
        BaseFont base = font.getBaseFont();

        int toPage = pdfStamper.getReader().getNumberOfPages();

        PdfContentByte content;
        Rectangle pageRect;
        for (int i = 1; i <= toPage; i++) {
            pageRect = pdfStamper.getReader().getPageSizeWithRotation(i);

            float x = pageRect.getWidth() / 2;
            float y = pageRect.getHeight() / 2;

            content = pdfStamper.getOverContent(i);
            content.saveState();
            // set Transparency
            content.setGState(gs);
            content.beginText();
            content.setColorFill(BaseColor.LIGHT_GRAY);
            content.setFontAndSize(base, 100);
            content.showTextAligned(Element.ALIGN_CENTER, watermark, x, y, rotation);
            content.endText();
        }
    }

    @Override
    public String convertToXhtml(String html) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        tidy.setWraplen(0);

        if (debugLogging) {
            System.out.println(html);
            tidy.setShowErrors(6);
            tidy.setQuiet(false);
            tidy.setShowWarnings(true);
        } else {
            tidy.setShowErrors(0);
            tidy.setQuiet(true);
            tidy.setShowWarnings(false);
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tidy.parseDOM(inputStream, outputStream);
        return outputStream.toString(UTF_8);
    }

}
