package com.farmconnect.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanyReportServiceTest {
    @Test void plainTextIsUnchanged() { assertEquals("Maize seed", CompanyReportService.cell("Maize seed")); }
    @Test void commasAndQuotesAreQuoted() { assertEquals("\"a, \"\"b\"\"\"", CompanyReportService.cell("a, \"b\"")); }
    @Test void formulasAreDefused() {
        assertEquals("'=SUM(A1:A9)", CompanyReportService.cell("=SUM(A1:A9)"));
        assertEquals("'@cmd", CompanyReportService.cell("@cmd"));
        assertEquals("'+1", CompanyReportService.cell("+1"));
    }
    @Test void nullBecomesEmpty() { assertEquals("", CompanyReportService.cell(null)); }
}
