package com.servlets;

import com.dao.AppDao;
import com.model.User;
import com.util.PasswordUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/login")
public class Login extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int AUTHENTICATED_SESSION_SECONDS = 30 * 60;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String status = request.getParameter("status");
        if ("registered".equals(status)) {
            request.setAttribute(
                    "statusMessage",
                    "Registration completed successfully. Please sign in.");
        } else if ("logged-out".equals(status)) {
            request.setAttribute("statusMessage", "You have been logged out securely.");
        } else if ("session-required".equals(status)) {
            request.setAttribute("statusMessage", "Please sign in to continue.");
        }

        request.getRequestDispatcher("/WEB-INF/views/login.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String submittedRollNumber = request.getParameter("rollNo");
        String password = request.getParameter("password");

        if (submittedRollNumber == null || submittedRollNumber.isBlank()
                || password == null || password.isBlank()) {
            showLoginError(request, response, "Enter your roll number and password.");
            return;
        }

        String rollNumber = submittedRollNumber.trim().toUpperCase();

        try {
            User user = new AppDao().findUser(rollNumber);
            if (user == null || !PasswordUtil.verify(password, user.passwordHash())) {
                showLoginError(request, response, "Invalid roll number or password.");
                return;
            }

            createAuthenticatedSession(request, user);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception exception) {
            getServletContext().log("Login failed", exception);
            showLoginError(
                    request,
                    response,
                    "Login is temporarily unavailable. Please try again shortly.");
        }
    }

    private void createAuthenticatedSession(HttpServletRequest request, User user) {
        HttpSession authenticatedSession = replaceSession(request);
        authenticatedSession.setAttribute("user", user);
        authenticatedSession.setMaxInactiveInterval(AUTHENTICATED_SESSION_SECONDS);
    }

    private HttpSession replaceSession(HttpServletRequest request) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        return request.getSession(true);
    }

    private void showLoginError(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws ServletException, IOException {

        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/login.jsp")
                .forward(request, response);
    }
}
