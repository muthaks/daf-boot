package za.co.rmb.approvalforum.service;

/**
 * The meeting invitation in the co-ds design system. Outlook keeps only inline styles, so these are
 * the resolved co-ds token values the mail service's co-ds email theme uses (named beside each),
 * and the invitation uses that email's inset details and accent.
 */
public final class InvitationStyle {

    // --co-font-primary
    public static final String FONT = "'RMB Sans', 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif";
    // --co-amber
    public static final String ACCENT = "#fa9d1e";
    // --co-bg-app
    public static final String PAGE_BG = "#fafafa";
    // --co-bg-surface
    public static final String CARD_BG = "#ffffff";
    // --co-text-primary
    public static final String TEXT = "#0a0a0d";
    // --co-text-muted
    public static final String TEXT_MUTED = "#4a4f55";
    // --co-border-subtle
    public static final String BORDER = "#ececec";

    private InvitationStyle() {
    }

    /** Text made safe for HTML; null becomes empty. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * The invitation body: the content in a bordered card and, when there is one, a button to the
     * meeting in Agenda Builder. Outlook shows the title, time and Teams details itself, so the card
     * carries no header. Padding sits on table cells, the only place Outlook keeps it.
     */
    public static String invitation(String content, String link) {
        StringBuilder sb = new StringBuilder()
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"640\" style=\"")
                .append("max-width:640px;width:100%;border-spacing:0;background:").append(CARD_BG)
                .append(";border:1px solid ").append(BORDER).append(";border-top:3px solid ").append(ACCENT)
                .append(";font-family:").append(FONT).append(";\">")
                .append("<tr><td style=\"padding:16px 20px 20px;font-family:").append(FONT)
                .append(";font-size:13px;line-height:1.5;color:").append(TEXT).append(";\">")
                .append(content);
        if (link != null) {
            sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td height=\"16\" ")
                    .append("style=\"height:16px;line-height:16px;font-size:0;\">&nbsp;</td></tr><tr>")
                    .append("<td style=\"background:").append(ACCENT).append(";padding:9px 20px;\">")
                    .append("<a href=\"").append(escape(link)).append("\" style=\"font-family:").append(FONT)
                    .append(";font-size:13px;font-weight:600;color:").append(TEXT)
                    .append(";text-decoration:none;\">View in Agenda Builder</a></td></tr></table>");
        }
        return sb.append("</td></tr></table><br>").toString();
    }

    /**
     * Label and value pairs, two to a row, inset on the page background as the email's details block.
     * Pairs without a value are left out; a pair with a third element ("wide") takes the whole row.
     */
    public static String details(String[][] pairs) {
        StringBuilder sb = new StringBuilder("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"")
                .append("border-collapse:collapse;table-layout:fixed;background:").append(PAGE_BG).append(";border:1px solid ")
                .append(BORDER).append(";font-size:12px;line-height:1.4;\">");
        int column = 0;
        for (String[] pair : pairs) {
            if (pair[1] == null || pair[1].trim().isEmpty()) {
                continue;
            }
            boolean wide = pair.length > 2;
            if (wide && column == 1) {
                sb.append("<td style=\"width:50%;\"></td></tr>");
                column = 0;
            }
            if (column == 0) {
                sb.append("<tr>");
            }
            sb.append(wide ? "<td colspan=\"2\" valign=\"top\" style=\"padding:6px 12px;\">"
                    : "<td valign=\"top\" style=\"width:50%;padding:6px 12px;\">")
                    .append("<span style=\"color:").append(TEXT_MUTED).append(";\">").append(escape(pair[0])).append("</span><br>")
                    .append("<span style=\"color:").append(TEXT).append(";font-weight:600;\">").append(escape(pair[1])).append("</span></td>");
            column = wide ? 0 : (column + 1) % 2;
            if (column == 0) {
                sb.append("</tr>");
            }
        }
        if (column == 1) {
            sb.append("<td style=\"width:50%;\"></td></tr>");
        }
        return sb.append("</table>").toString();
    }

    /** An agenda item: its number and counterparty, with the time beside it, then its details. */
    public static String item(String title, String aside, String content) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr>"
                + "<td height=\"16\" style=\"height:16px;line-height:16px;font-size:0;\">&nbsp;</td></tr><tr>"
                + "<td style=\"border-left:3px solid " + ACCENT + ";padding:2px 0 2px 10px;font-family:" + FONT
                + ";font-size:14px;line-height:1.4;font-weight:600;color:" + TEXT + ";\">" + escape(title)
                + (aside == null ? "" : "<span style=\"font-size:12px;font-weight:400;color:" + TEXT_MUTED + ";\"> &nbsp;"
                + escape(aside) + "</span>")
                + "</td></tr><tr><td height=\"8\" style=\"height:8px;line-height:8px;font-size:0;\">&nbsp;</td></tr></table>"
                + content;
    }

    /** A muted note under the agenda. */
    public static String note(String text) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr>"
                + "<td style=\"padding-top:14px;font-family:" + FONT + ";font-size:12px;color:" + TEXT_MUTED + ";\">"
                + escape(text) + "</td></tr></table>";
    }
}
