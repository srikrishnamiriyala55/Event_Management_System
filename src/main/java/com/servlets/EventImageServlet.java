package com.servlets;

import com.dao.AppDao;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/event-image")
public class EventImageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int eventId = Integer.parseInt(request.getParameter("id"));
            AppDao appDao = new AppDao();
            byte[] image = appDao.eventImage(eventId);

            if (image == null || image.length == 0) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String imageType = appDao.eventImageType(eventId);
            response.setContentType(imageType == null ? "image/jpeg" : imageType);
            response.setContentLength(image.length);
            response.setHeader("Cache-Control", "private, max-age=3600");
            response.getOutputStream().write(image);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event ID.");
        } catch (Exception exception) {
            throw new ServletException("Unable to load event image.", exception);
        }
    }
}
