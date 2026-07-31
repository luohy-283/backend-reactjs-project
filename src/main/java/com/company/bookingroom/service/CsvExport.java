package com.company.bookingroom.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Build UTF-8 CSV with BOM for Excel. */
public final class CsvExport {

    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private CsvExport() {}

    public static byte[] toUtf8BomBytes(String csvBody) {
        byte[] body = csvBody.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String line(Object... cells) {
        List<String> parts = new ArrayList<>(cells.length);
        for (Object cell : cells) {
            parts.add(escape(cell));
        }
        return String.join(",", parts);
    }
}
