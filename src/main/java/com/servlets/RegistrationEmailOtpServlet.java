package com.servlets;

import com.service.EmailService;

import java.io.IOException;
import java.security.SecureRandom;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/registration-email-otp")
public class RegistrationEmailOtpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long OTP_VALIDITY_MILLIS = 5 * 60 * 1_000L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        String email = request.getParameter("email");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            writeResponse(response, false, "Enter a valid email address.");
            return;
        }

        HttpSession session = request.getSession(true);
        try {
            if ("send".equals(action)) {
                sendOtp(session, email);
                writeResponse(response, true, "OTP sent successfully. Please check your email.");
                return;
            }

            if ("verify".equals(action) && isValidOtp(request, session, email)) {
                session.setAttribute("verifiedRegistrationEmail", email.toLowerCase());
                clearPendingOtp(session);
                writeResponse(
                        response,
                        true,
                        "Email verified successfully. You can now submit the form.");
                return;
            }

            writeResponse(response, false, "The OTP is invalid or has expired.");
        } catch (Exception exception) {
            getServletContext().log("Registration email OTP failed", exception);
            writeResponse(
                    response,
                    false,
                    "The OTP service is temporarily unavailable. Please try again.");
        }
    }

    private void sendOtp(HttpSession session, String email) throws Exception {
        String otp = String.format(
                "%06d",
                new SecureRandom().nextInt(1_000_000));

        session.setAttribute("registrationOtp", otp);
        session.setAttribute("registrationOtpEmail", email.toLowerCase());
        session.setAttribute(
                "registrationOtpExpires",
                System.currentTimeMillis() + OTP_VALIDITY_MILLIS);
        session.removeAttribute("verifiedRegistrationEmail");

        EmailService.sendRegistrationOtp(email, otp);
    }

    private boolean isValidOtp(
            HttpServletRequest request,
            HttpSession session,
            String email) {

        String submittedOtp = request.getParameter("otp");
        String expectedOtp = (String) session.getAttribute("registrationOtp");
        String expectedEmail = (String) session.getAttribute("registrationOtpEmail");
        Long expiresAt = (Long) session.getAttribute("registrationOtpExpires");

        return expiresAt != null
                && System.currentTimeMillis() <= expiresAt
                && expectedEmail != null
                && email.equalsIgnoreCase(expectedEmail)
                && submittedOtp != null
                && submittedOtp.equals(expectedOtp);
    }

    private void clearPendingOtp(HttpSession session) {
        session.removeAttribute("registrationOtp");
        session.removeAttribute("registrationOtpEmail");
        session.removeAttribute("registrationOtpExpires");
    }

    private void writeResponse(
            HttpServletResponse response,
            boolean successful,
            String message) throws IOException {

        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write(
                "{\"success\":" + successful
                        + ",\"message\":\"" + safeMessage + "\"}");
    }
}
