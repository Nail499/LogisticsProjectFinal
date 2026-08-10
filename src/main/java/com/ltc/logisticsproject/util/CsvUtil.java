package com.ltc.logisticsproject.util;

import java.util.List;

// Sadə CSV yazıcı — admin hesabatlarının Excel-də açıla bilən ixracı üçün
// (bax AdminExportController). Ayrıca Apache POI kimi ağır asılılıq əlavə
// etmədən UTF-8 BOM-lu CSV, Excel-in Azərbaycan hərflərini (ə, ö, ü, ş, ç,
// ğ, ı) düzgün göstərməsi üçün kifayətdir — Excel .csv faylını birbaşa aça
// bilir və sütunlara ayırır.
public class CsvUtil {

    private static final String BOM = "﻿";

    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(",", headers.stream().map(CsvUtil::escape).toList())).append("\n");
        for (List<String> row : rows) {
            sb.append(String.join(",", row.stream().map(CsvUtil::escape).toList())).append("\n");
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
