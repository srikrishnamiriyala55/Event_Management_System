package com.servlets;

import com.model.User;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/verify-otp")
public class OtpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int AUTHENTICATED_SESSION_SECONDS = 30 * 60;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("pendingUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login?status=session-required");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/verify-otp.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String submittedOtp = request.getParameter("otp");
        String expectedOtp = session == null
                ? null
                : (String) session.getAttribute("loginOtp");
        Long expiresAt = session == null
                ? null
                : (Long) session.getAttribute("otpExpiresAt");

        boolean valid = expectedOtp != null
                && expiresAt != null
                && System.currentTimeMillis() <= expiresAt
                && expectedOtp.equals(submittedOtp);

        if (!valid) {
            request.setAttribute("errorMessage", "The OTP is invalid or has expired.");
            request.getRequestDispatcher("/WEB-INF/views/verify-otp.jsp")
                    .forward(request, response);
            return;
        }

        User organizer = (User) session.getAttribute("pendingUser");
        session.invalidate();

        HttpSession authenticatedSession = request.getSession(true);
        authenticatedSession.setAttribute("user", organizer);
        authenticatedSession.setMaxInactiveInterval(AUTHENTICATED_SESSION_SECONDS);

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
