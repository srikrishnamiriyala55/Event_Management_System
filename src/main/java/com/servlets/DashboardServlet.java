package com.servlets;

import com.dao.AppDao;
import com.model.Event;
import com.model.ParticipantEventRegistration;
import com.model.User;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {
    "/dashboard",
    "/events",
    "/registrations",
    "/profile"
})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            AppDao appDao = new AppDao();
            User user = (User) request.getSession().getAttribute("user");
            request.setAttribute("activeSection", activeSection(request));

            if ("PARTICIPANT".equals(user.role())) {
                List<Event> availableEvents = appDao.events(true);
                List<ParticipantEventRegistration> registrations =
                        appDao.participantRegistrations(user.rollNo());

                if ("events".equals(request.getAttribute("activeSection"))) {
                    availableEvents = filterEvents(
                            availableEvents,
                            request.getParameter("category"),
                            request.getParameter("department"));
                } else if ("registrations".equals(
                        request.getAttribute("activeSection"))) {
                    registrations = filterRegistrations(
                            registrations,
                            request.getParameter("category"),
                            request.getParameter("department"));
                }

                request.setAttribute("availableEvents", availableEvents);
                request.setAttribute(
                        "participantRegistrations",
                        registrations);
                request.getRequestDispatcher("/WEB-INF/views/userdashboard.jsp")
                        .forward(request, response);
                return;
            }

            if ("registrations".equals(request.getAttribute("activeSection"))) {
                response.sendRedirect(request.getContextPath() + "/reports");
                return;
            }

            List<Event> events = appDao.events(false);
            if ("events".equals(request.getAttribute("activeSection"))) {
                events = filterEvents(
                        events,
                        request.getParameter("category"),
                        request.getParameter("department"));
            }
            request.setAttribute("events", events);
            request.getRequestDispatcher("/WEB-INF/views/organizerdashboard.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException("Unable to load dashboard.", exception);
        }
    }

    private String activeSection(HttpServletRequest request) {
        return switch (request.getServletPath()) {
            case "/events" -> "events";
            case "/registrations" -> "registrations";
            case "/profile" -> "profile";
            default -> "dashboard";
        };
    }

    private List<Event> filterEvents(
            List<Event> events,
            String category,
            String department) {

        return events.stream()
                .filter(event -> category == null
                        || category.isBlank()
                        || category.equals(event.category()))
                .filter(event -> department == null
                        || department.isBlank()
                        || department.equals(event.department()))
                .toList();
    }

    private List<ParticipantEventRegistration> filterRegistrations(
            List<ParticipantEventRegistration> registrations,
            String category,
            String department) {

        return registrations.stream()
                .filter(registration -> category == null
                        || category.isBlank()
                        || category.equals(registration.event().category()))
                .filter(registration -> department == null
                        || department.isBlank()
                        || department.equals(registration.event().department()))
                .toList();
    }
}
