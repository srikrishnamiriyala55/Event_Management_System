package com.servlets;

import com.dao.AppDao;
import com.model.Event;
import com.model.User;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/registration")
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User participant = (User) request.getSession().getAttribute("user");
        if (!"PARTICIPANT".equals(participant.role())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String redirectPath = "/events";
        try {
            int eventId = Integer.parseInt(request.getParameter("eventId"));
            AppDao appDao = new AppDao();
            Event event = appDao.event(eventId);

            if (event == null) {
                request.getSession().setAttribute("flash", "Event not found.");
                response.sendRedirect(request.getContextPath() + redirectPath);
                return;
            }

            String passId = appDao.register(eventId, participant.rollNo());

            request.getSession().setAttribute(
                    "flash",
                    "Registration complete. Pass ID: " + passId);
            redirectPath = "/registrations";
        } catch (NumberFormatException exception) {
            request.getSession().setAttribute("flash", "Invalid event selection.");
        } catch (SQLException exception) {
            handleRegistrationDatabaseError(request, exception);
        } catch (Exception exception) {
            getServletContext().log("Event registration failed", exception);
            request.getSession().setAttribute(
                    "flash",
                    "Registration failed. Please try again.");
        }

        response.sendRedirect(request.getContextPath() + redirectPath);
    }

    private void handleRegistrationDatabaseError(
            HttpServletRequest request,
            SQLException exception) {

        String message = exception.getMessage() == null
                ? ""
                : exception.getMessage().toLowerCase();

        if (message.contains("duplicate")) {
            request.getSession().setAttribute(
                    "flash",
                    "You are already registered for this event.");
            return;
        }

        if (message.contains("full or unavailable")) {
            request.getSession().setAttribute(
                    "flash",
                    "This event is full or no longer available for registration.");
            return;
        }

        getServletContext().log("Event registration failed", exception);
        request.getSession().setAttribute(
                "flash",
                "Registration failed. Please try again.");
    }

}
