package com.farmconnect.report;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A tiny PDF writer: A4 pages, Helvetica text, rectangles and lines. No libraries needed.
 * Coordinates are PDF points with the origin at the bottom-left of the page.
 */
public final class SimplePdf {
    public static final float W = 595.28f, H = 841.89f;

    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder cur;
    private final String title;

    public SimplePdf(String title) {
        this.title = title == null ? "" : title;
        newPage();
    }

    public void newPage() { cur = new StringBuilder(); pages.add(cur); }
    public int pageCount() { return pages.size(); }
    /** Switch drawing to an existing page (used to add footers once the page count is known). */
    public void page(int index) { cur = pages.get(index); }

    public void rect(float x, float y, float w, float h, int rgb) {
        cur.append(color(rgb, false)).append(' ').append(n(x)).append(' ').append(n(y)).append(' ')
           .append(n(w)).append(' ').append(n(h)).append(" re f\n");
    }

    public void line(float x1, float y1, float x2, float y2, float width, int rgb) {
        cur.append(color(rgb, true)).append(' ').append(n(width)).append(" w ").append(n(x1)).append(' ').append(n(y1))
           .append(" m ").append(n(x2)).append(' ').append(n(y2)).append(" l S\n");
    }

    public void text(float x, float y, float size, boolean bold, int rgb, String s) {
        if (s == null || s.isEmpty()) return;
        cur.append("BT ").append(bold ? "/F2 " : "/F1 ").append(n(size)).append(" Tf ").append(color(rgb, false))
           .append(' ').append(n(x)).append(' ').append(n(y)).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    /** Approximate width of a string in points (good enough for right-aligning numbers and truncating labels). */
    public static float width(String s, float size, boolean bold) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            float f;
            if (c >= '0' && c <= '9') f = 0.556f;
            else if (c == ' ' || c == ',' || c == '.' || c == ':' || c == ';' || c == '|' || c == '!' || c == 'i' || c == 'l' || c == 'j' || c == 'I' || c == 't' || c == 'f') f = 0.28f;
            else if (c == 'm' || c == 'M' || c == 'W' || c == 'w') f = 0.85f;
            else if (Character.isUpperCase(c)) f = 0.68f;
            else f = 0.53f;
            w += f;
        }
        return w * size * (bold ? 1.06f : 1f);
    }

    /** Shorten a string with "..." so it fits in maxWidth points. */
    public static String fit(String s, float size, boolean bold, float maxWidth) {
        if (s == null) return "";
        if (width(s, size, bold) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && width(t + "...", size, bold) > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        int pageCount = pages.size();
        // object numbers: 1 catalog, 2 pages, 3 regular font, 4 bold font, 5 info, then (page, content) pairs
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) kids.append(6 + i * 2).append(" 0 R ");

        obj(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offsets, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageCount + " >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        obj(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        obj(out, offsets, "<< /Title (" + escape(title) + ") /Creator (FarmConnect) /Producer (FarmConnect) >>");
        for (int i = 0; i < pageCount; i++) {
            int contentNo = 7 + i * 2;
            obj(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(W) + " " + n(H) + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentNo + " 0 R >>");
            byte[] body = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + body.length + " >>\nstream\n");
            out.write(body, 0, body.length);
            write(out, "\nendstream\nendobj\n");
        }
        int xref = out.size();
        int total = offsets.size() + 1;
        write(out, "xref\n0 " + total + "\n0000000000 65535 f \n");
        for (int off : offsets) write(out, String.format(Locale.US, "%010d 00000 n \n", off));
        write(out, "trailer\n<< /Size " + total + " /Root 1 0 R /Info 5 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    private static String n(float v) { return String.format(Locale.US, "%.2f", v); }

    private static String color(int rgb, boolean stroke) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        return String.format(Locale.US, "%.3f %.3f %.3f %s", r, g, b, stroke ? "RG" : "rg");
    }

    /** Keep only what WinAnsi can show; escape the characters PDF strings treat as special. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\u2013': case '\u2014': c = '-'; break;
                case '\u2018': case '\u2019': c = '\''; break;
                case '\u201c': case '\u201d': c = '"'; break;
                case '\u2713': c = '+'; break;
                default: break;
            }
            if (c == '\\' || c == '(' || c == ')') sb.append('\\').append(c);
            else if (c < 32) sb.append(' ');
            else if (c > 255) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }
}
