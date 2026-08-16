package com.artajerjes.biwengerassistant.biwenger.dto.report;

import java.util.List;

public record BiwengerReportData(
        List<BiwengerReportColumn> columns,
        List<List<Object>> rows,
        Object settings) {
}