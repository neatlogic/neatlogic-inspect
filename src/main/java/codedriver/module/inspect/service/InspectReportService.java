package codedriver.module.inspect.service;

import org.bson.Document;

public interface InspectReportService {

    Document getInspectReport(Long resourceId, String id);
}
