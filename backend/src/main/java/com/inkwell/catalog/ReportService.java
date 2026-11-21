package com.inkwell.catalog;

import com.inkwell.catalog.CatalogDtos.ItemDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Printable catalog report (HTML) and spreadsheet export (CSV). */
@Service
public class ReportService {
    private final CatalogService catalog;

    public ReportService(CatalogService catalog) { this.catalog = catalog; }

    public String html() {
        List<ItemDto> all = catalog.all();
        StringBuilder sb = new StringBuilder("""
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Inkwell catalog report</title>
                <style>body{font:14px/1.5 system-ui,sans-serif;margin:2rem;color:#1b2333}
                table{border-collapse:collapse;width:100%}th,td{padding:.45rem .7rem;border-bottom:1px solid #d9ddd6;text-align:left}
                th{background:#eef0ec}td.n{text-align:right;font-variant-numeric:tabular-nums}h1{margin:0 0 .25rem}
                @media print{body{margin:0}}</style></head><body>
                <h1>Inkwell catalog</h1><p>""");
        sb.append(all.size()).append(" items</p><table><thead><tr><th>Title</th><th>Authors</th><th>Category</th>")
          .append("<th>Year</th><th class=\"n\">Price</th><th class=\"n\">Stock</th></tr></thead><tbody>");
        for (ItemDto i : all) {
            sb.append("<tr><td>").append(esc(i.title())).append("</td><td>").append(esc(String.join(", ", i.authors())))
              .append("</td><td>").append(esc(i.category())).append("</td><td>")
              .append(i.publishedYear() == null ? "" : i.publishedYear())
              .append("</td><td class=\"n\">").append(i.price() == null ? "" : i.price().toPlainString())
              .append("</td><td class=\"n\">").append(i.stock()).append("</td></tr>");
        }
        return sb.append("</tbody></table></body></html>").toString();
    }

    public String csv() {
        StringBuilder sb = new StringBuilder("\uFEFFid,title,authors,category,isbn,year,price,stock\n");
        for (ItemDto i : catalog.all()) {
            sb.append(i.id()).append(',').append(q(i.title())).append(',').append(q(String.join("; ", i.authors())))
              .append(',').append(q(i.category())).append(',').append(q(i.isbn() == null ? "" : i.isbn())).append(',')
              .append(i.publishedYear() == null ? "" : i.publishedYear()).append(',')
              .append(i.price() == null ? "" : i.price().toPlainString()).append(',').append(i.stock()).append('\n');
        }
        return sb.toString();
    }

    private static String esc(String s) { return HtmlUtils.htmlEscape(s == null ? "" : s); }

    /** CSV field with quote escaping and protection against spreadsheet formula injection. */
    private static String q(String s) {
        String v = s.replace("\"", "\"\"");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v + "\"";
    }
}
