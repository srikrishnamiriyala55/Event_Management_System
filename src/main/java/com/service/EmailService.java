package com.service;

import com.dao.Dao;
import com.model.Event;
import com.model.User;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public final class EmailService {

    private static final String SMTP_APP_PASSWORD = "vorcbnzvmnzzekhf";
    private static final DateTimeFormatter EVENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a");

    private EmailService() {
    }

    public static void sendRegistrationOtp(String email, String otp)
            throws MessagingException {

        String content = paragraph(
                "We received a request to verify this email address for a new "
                        + "Campus Events participant account.")
                + codePanel("Verification code", otp)
                + paragraph("This code is valid for 5 minutes. Do not share it with anyone.")
                + securityNotice(
                        "If you did not request this registration, you may safely ignore this email.");

        sendHtml(
                email,
                "Verify your Campus Events email address",
                "Participant email verification",
                "Complete your email verification",
                content);
    }

    public static void sendOrganizerLoginOtp(User organizer, String otp)
            throws MessagingException {

        String content = paragraph(
                "Hello " + escapeHtml(organizer.fullName())
                        + ", use the secure code below to complete your organizer login.")
                + codePanel("Organizer login code", otp)
                + paragraph("This code expires in 5 minutes and can be used only once.")
                + securityNotice(
                        "If you did not attempt to sign in, please change your password and "
                                + "contact the system administrator.");

        sendHtml(
                organizer.email(),
                "Your Campus Events organizer login code",
                "Secure organizer sign-in",
                "Organizer login verification",
                content);
    }

    public static void sendParticipantWelcome(User participant)
            throws MessagingException {

        String content = paragraph(
                "Hello " + escapeHtml(participant.fullName())
                        + ", your participant account has been created successfully.")
                + detailTable(
                        new String[][] {
                            {"Roll number / Username", participant.rollNo()},
                            {"Registered email", participant.email()},
                            {"Department", participant.department()},
                            {"Study year", String.valueOf(participant.year())}
                        })
                + paragraph(
                        "You can now sign in with your roll number and password to browse "
                                + "available campus events.");

        sendHtml(
                participant.email(),
                "Welcome to Campus Events",
                "Participant registration completed",
                "Registration successful",
                content);
    }

    public static void sendEventRegistrationReceipt(
            User participant,
            Event event,
            String passId) throws MessagingException {

        String content = paragraph(
                "Hello " + escapeHtml(participant.fullName())
                        + ", your event registration and payment have been confirmed.")
                + detailTable(
                        new String[][] {
                            {"Event", event.title()},
                            {"Date and time", event.date().format(EVENT_DATE_FORMAT)},
                            {"Venue", event.venue()},
                            {"Pass ID", passId},
                            {"Payment status", "PAID"},
                            {"Amount", String.format("₹%.2f", event.fee())}
                        })
                + securityNotice(
                        "Please keep this email or your Pass ID available when entering the event.");

        sendHtml(
                participant.email(),
                "Event registration confirmed — " + event.title(),
                "Event registration receipt",
                "Your event registration is confirmed",
                content);
    }

    private static void sendHtml(
            String recipient,
            String subject,
            String eyebrow,
            String heading,
            String content) throws MessagingException {

        String smtpHost = Dao.env("SMTP_HOST", "smtp.gmail.com");
        String smtpUser = Dao.env("SMTP_USER", "nitwg6@gmail.com");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", Dao.env("SMTP_PORT", "587"));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");

        Session mailSession = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, SMTP_APP_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(mailSession);
        InternetAddress sender = new InternetAddress(Dao.env("SMTP_FROM", smtpUser));

        try {
            sender.setPersonal("Campus Events", StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new MessagingException("Unable to create the sender address", exception);
        }

        message.setFrom(sender);
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(recipient));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setContent(emailTemplate(eyebrow, heading, content), "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private static String emailTemplate(String eyebrow, String heading, String content) {
        return "<!doctype html><html><body style=\"margin:0;background:#f3f6f8;"
                + "font-family:Arial,sans-serif;color:#17212b\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:28px 16px\">"
                + "<div style=\"background:#12355b;color:#fff;padding:20px 24px;"
                + "border-radius:8px 8px 0 0\">"
                + "<div style=\"font-size:12px;letter-spacing:1px;text-transform:uppercase;"
                + "opacity:.85\">" + escapeHtml(eyebrow) + "</div>"
                + "<div style=\"font-size:24px;font-weight:700;margin-top:6px\">"
                + escapeHtml(heading) + "</div></div>"
                + "<div style=\"background:#fff;border:1px solid #d8e1e8;border-top:0;"
                + "padding:26px 24px;border-radius:0 0 8px 8px\">"
                + content
                + "<p style=\"margin:26px 0 0;color:#52616f;font-size:13px;"
                + "border-top:1px solid #d8e1e8;padding-top:18px\">"
                + "Campus Event Management System<br>Automated message — please do not reply."
                + "</p></div></div></body></html>";
    }

    private static String paragraph(String text) {
        return "<p style=\"font-size:15px;line-height:1.6;margin:0 0 18px\">"
                + text + "</p>";
    }

    private static String codePanel(String label, String code) {
        return "<div style=\"background:#eef4f8;border:1px solid #d8e1e8;"
                + "border-radius:6px;padding:18px;text-align:center;margin:18px 0\">"
                + "<div style=\"font-size:12px;color:#52616f;text-transform:uppercase;"
                + "letter-spacing:.8px\">" + escapeHtml(label) + "</div>"
                + "<div style=\"font-size:30px;font-weight:700;letter-spacing:6px;"
                + "color:#12355b;margin-top:8px\">" + escapeHtml(code) + "</div></div>";
    }

    private static String detailTable(String[][] rows) {
        StringBuilder table = new StringBuilder(
                "<table role=\"presentation\" style=\"width:100%;border-collapse:collapse;"
                        + "margin:18px 0\">");

        for (String[] row : rows) {
            table.append("<tr><td style=\"padding:10px;border-bottom:1px solid #d8e1e8;"
                    + "color:#52616f;width:42%\">")
                    .append(escapeHtml(row[0]))
                    .append("</td><td style=\"padding:10px;border-bottom:1px solid #d8e1e8;"
                            + "font-weight:600\">")
                    .append(escapeHtml(row[1]))
                    .append("</td></tr>");
        }

        return table.append("</table>").toString();
    }

    private static String securityNotice(String text) {
        return "<div style=\"background:#fff8e6;border-left:4px solid #9a7116;"
                + "padding:12px 14px;font-size:13px;line-height:1.5\">"
                + text + "</div>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
