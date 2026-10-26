package com.farmconnect.report;

import com.farmconnect.dto.Community.Analytics;
import com.farmconnect.dto.Community.Indicators;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The county report as an Excel workbook (.xlsx), written directly (an .xlsx is a zip of small XML files) so no
 * extra library is needed. Sheets: Summary, Sub-counties, Products, Sales, Harvest, Groups.
 */
public final class ReportXlsx {
    private ReportXlsx() {}

    private record Sheet(String name, List<List<Object>> rows, int[] widths) {}

    public static byte[] build(Analytics a, String generatedFor) {
        List<Sheet> sheets = new ArrayList<>();
        Indicators i = a.indicators();
        List<List<Object>> sum = new ArrayList<>();
        sum.add(List.of("FarmConnect report", a.scopeLabel()));
        sum.add(List.of("Prepared for", generatedFor == null ? "" : generatedFor));
        sum.add(List.of());
        sum.add(List.of("Indicator", "Value"));
        sum.add(List.of("Registered farmers", i.registeredFarmers()));
        sum.add(List.of("Verified farmers", i.verifiedFarmers()));
        sum.add(List.of("Registered buyers", i.registeredBuyers()));
        sum.add(List.of("Active users (30 days)", i.activeUsers30d()));
        sum.add(List.of("Farmer groups", i.groupsOnboarded()));
        sum.add(List.of("Group members", i.groupMembers()));
        sum.add(List.of("Market listings", i.marketListings()));
        sum.add(List.of("Farmer-to-buyer connections", i.farmerBuyerConnections()));
        sum.add(List.of("Officers active (30 days)", i.officersActive30d()));
        sum.add(List.of("Farmers using digital records", i.farmersUsingRecords()));
        sum.add(List.of("Farm records created", i.recordsTotal()));
        sum.add(List.of("Farmer satisfaction (1-5)", Math.round(i.satisfactionAverage() * 100.0) / 100.0));
        sheets.add(new Sheet("Summary", sum, new int[]{38, 26}));

        List<List<Object>> sub = new ArrayList<>();
        sub.add(List.of("Sub-county", "Farmers", "Verified farmers"));
        a.farmersBySubCounty().forEach((k, v) -> sub.add(List.of(k, v, a.verifiedBySubCounty().getOrDefault(k, 0L))));
        sheets.add(new Sheet("Sub-counties", sub, new int[]{28, 12, 18}));

        sheets.add(new Sheet("Products", two("Category", "Live products", a.productsByCategory()), new int[]{30, 16}));
        List<List<Object>> sales = new ArrayList<>();
        sales.add(List.of("Month", "Orders", "Sales (KES)"));
        a.ordersByMonth().forEach((k, v) -> {
            BigDecimal s = a.salesByMonth().get(k);
            sales.add(List.of(k, v, s == null ? 0 : s.doubleValue()));
        });
        sheets.add(new Sheet("Sales", sales, new int[]{14, 12, 18}));
        sheets.add(new Sheet("Harvest", two("Crop", "Recorded harvest (kg)", a.harvestKgByCrop()), new int[]{26, 24}));
        sheets.add(new Sheet("Groups", two("Group", "Members", a.membersByGroup()), new int[]{34, 12}));
        return write(sheets);
    }

    private static List<List<Object>> two(String h1, String h2, Map<String, Long> data) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of(h1, h2));
        data.forEach((k, v) -> rows.add(List.of(k, v)));
        return rows;
    }

    // ---------------------------------------------------------------- xlsx writer

    private static byte[] write(List<Sheet> sheets) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream z = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
                StringBuilder ct = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
                for (int n = 1; n <= sheets.size(); n++)
                    ct.append("<Override PartName=\"/xl/worksheets/sheet").append(n).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                ct.append("</Types>");
                put(z, "[Content_Types].xml", ct.toString());
                put(z, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                StringBuilder wb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
                StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                for (int n = 1; n <= sheets.size(); n++) {
                    wb.append("<sheet name=\"").append(esc(sheets.get(n - 1).name())).append("\" sheetId=\"").append(n).append("\" r:id=\"rId").append(n).append("\"/>");
                    rels.append("<Relationship Id=\"rId").append(n).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(n).append(".xml\"/>");
                }
                wb.append("</sheets></workbook>");
                rels.append("<Relationship Id=\"rId").append(sheets.size() + 1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
                put(z, "xl/workbook.xml", wb.toString());
                put(z, "xl/_rels/workbook.xml.rels", rels.toString());
                put(z, "xl/styles.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>"
                        + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
                        + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF2E7D32\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                        + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs>"
                        + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                        + "</styleSheet>");
                for (int n = 1; n <= sheets.size(); n++) put(z, "xl/worksheets/sheet" + n + ".xml", sheetXml(sheets.get(n - 1)));
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the Excel report", e);
        }
    }

    private static String sheetXml(Sheet s) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>");
        for (int c = 0; c < s.widths().length; c++)
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"").append(s.widths()[c]).append("\" customWidth=\"1\"/>");
        sb.append("</cols><sheetData>");
        // the header row is the first row whose first cell is not the title lines
        int headerRow = s.name().equals("Summary") ? 4 : 1;
        for (int r = 0; r < s.rows().size(); r++) {
            List<Object> row = s.rows().get(r);
            if (row.isEmpty()) continue;
            sb.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.size(); c++) {
                String ref = col(c) + (r + 1);
                Object v = row.get(c);
                String style = (r + 1 == headerRow) ? " s=\"1\"" : "";
                if (v instanceof Number num) sb.append("<c r=\"").append(ref).append("\"").append(style).append("><v>").append(num).append("</v></c>");
                else sb.append("<c r=\"").append(ref).append("\"").append(style).append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(esc(String.valueOf(v))).append("</t></is></c>");
            }
            sb.append("</row>");
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static String col(int c) { return String.valueOf((char) ('A' + c)); }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '&') sb.append("&amp;"); else if (ch == '<') sb.append("&lt;"); else if (ch == '>') sb.append("&gt;");
            else if (ch == '"') sb.append("&quot;"); else if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') continue; else sb.append(ch);
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream z, String name, String content) throws java.io.IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}
