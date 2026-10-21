package com.servlets;

import com.dao.AppDao;
import com.model.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        if (!"ORGANIZER".equals(user.role())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            AppDao appDao = new AppDao();
            String eventId = request.getParameter("eventId");
            String year = request.getParameter("year");

            request.setAttribute("events", appDao.events(false));
            request.setAttribute("activeSection", "reports");
            request.setAttribute("records", appDao.report(
                    eventId == null || eventId.isBlank() ? null : Integer.valueOf(eventId),
                    request.getParameter("collegeName"),
                    request.getParameter("department"),
                    year == null || year.isBlank() ? null : Integer.valueOf(year),
                    request.getParameter("search")));

            request.getRequestDispatcher("/WEB-INF/views/organizerdashboard.jsp")
                    .forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Event and year filters must contain valid numbers.");
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
