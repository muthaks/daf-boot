package za.co.rmb.approvalforum.docgen.logic;

import java.io.UnsupportedEncodingException;

import com.google.common.io.ByteSource;
import za.co.rmb.approvalforum.docgen.model.PingModel;
import za.co.rmb.approvalforum.docgen.model.ReportOutputModel;
import za.co.rmb.approvalforum.docgen.model.AshburtonReportOutputModel;
public interface DocGenService {

    ByteSource generateReport(ReportOutputModel reportOutputModel);

    PingModel ping();

    String convertToXhtml(String html) throws UnsupportedEncodingException;

    ByteSource generateASBReport(AshburtonReportOutputModel reportOutputModel);
    //generateBroaderAfricaReport
    ByteSource generateBroaderAfricaReport(ReportOutputModel reportOutputModel);
}
