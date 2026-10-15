package za.co.rmb.approvalforum.docgen.controller;

import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import za.co.rmb.approvalforum.docgen.logic.DocGenService;
import za.co.rmb.approvalforum.docgen.model.PingModel;
import za.co.rmb.approvalforum.docgen.model.ReportOutputModel;
import za.co.rmb.approvalforum.docgen.model.AshburtonReportOutputModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class DocGenController {

    @Autowired
    private DocGenService docGenService;

    private static final Logger logger = LoggerFactory.getLogger(DocGenController.class);

    @PostMapping(value = "/generate", consumes = "application/json")
    public ResponseEntity<byte[]> generatePdf(@RequestBody ReportOutputModel model) {
        ByteSource msg = docGenService.generateReport(model);
        byte[] responseBody = null;

        if (null == msg) {
            return ResponseEntity.badRequest().body(responseBody);
        } else {
            try {
                responseBody = msg.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DAF.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(responseBody);
    }

    @PostMapping(value = "/generateASB", consumes = "application/json")
    public ResponseEntity<byte[]> generateASBPdf(@RequestBody AshburtonReportOutputModel model) {

        logger.info("generateASBPdf(START)");

        ByteSource msg = docGenService.generateASBReport(model);
        byte[] responseBody = null;

        if (null == msg) {
            return ResponseEntity.badRequest().body(responseBody);
        } else {
            try {
                responseBody = msg.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("generateASBPdf(END)");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DAF.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(responseBody);
    }
    ///generatePdfForBroaderAfrica
    @PostMapping(value = "/generatePdfForBroaderAfrica", consumes = "application/json")
    public ResponseEntity<byte[]> generatePdfForBroaderAfrica(@RequestBody ReportOutputModel model) {

        logger.info("generatePdfForBroaderAfrica(START)");

        ByteSource msg = docGenService.generateBroaderAfricaReport(model);
        byte[] responseBody = null;

        if (null == msg) {
            return ResponseEntity.badRequest().body(responseBody);
        } else {
            try {
                responseBody = msg.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("generateASBPdf(END)");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DAF.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(responseBody);
    }

    @GetMapping("/ping")
    public PingModel ping() {
        return docGenService.ping();
    }

}
