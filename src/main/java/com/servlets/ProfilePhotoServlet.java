package com.servlets;

import com.dao.AppDao;
import com.model.User;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/profile-photo")
public class ProfilePhotoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User viewer = (User) request.getSession().getAttribute("user");
        String requestedRollNumber = request.getParameter("rollNo");
        String rollNumber = requestedRollNumber == null || requestedRollNumber.isBlank()
                ? viewer.rollNo()
                : requestedRollNumber.trim().toUpperCase();

        boolean canView = "ORGANIZER".equals(viewer.role())
                || viewer.rollNo().equals(rollNumber);
        if (!canView) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            AppDao appDao = new AppDao();
            byte[] profilePhoto = appDao.profilePhoto(rollNumber);
            if (profilePhoto == null || profilePhoto.length == 0) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = appDao.profilePhotoType(rollNumber);
            response.setContentType(contentType == null ? "image/jpeg" : contentType);
            response.setContentLength(profilePhoto.length);
            response.setHeader("Cache-Control", "private, max-age=300");
            response.getOutputStream().write(profilePhoto);
        } catch (Exception exception) {
            getServletContext().log("Profile photo load failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
