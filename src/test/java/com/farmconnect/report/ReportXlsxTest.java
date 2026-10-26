package com.farmconnect.report;

import com.farmconnect.dto.Community.Analytics;
import com.farmconnect.dto.Community.Indicators;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReportXlsxTest {
    @Test void buildsAWorkbookWithAllSheets() throws Exception {
        Indicators i = new Indicators(10, 6, 4, 8, 2, 30, 5, 12, 3, 1, 2, 1, 0, 4.5, 9, 7, 25, 40);
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Endebess", 5L);
        Analytics a = new Analytics(i, m, m, m, m, m, m, Map.of("2026-04", new BigDecimal("1500")), m, m, m, m, true, "Trans Nzoia County");
        byte[] bytes = ReportXlsx.build(a, "Test & <User>");
        int sheets = 0;
        boolean workbook = false;
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                if (e.getName().startsWith("xl/worksheets/sheet")) sheets++;
                if (e.getName().equals("xl/workbook.xml")) workbook = true;
            }
        }
        assertTrue(workbook);
        assertEquals(6, sheets);
        assertTrue(bytes.length > 1000);
    }
}
