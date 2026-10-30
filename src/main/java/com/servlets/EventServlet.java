package com.servlets;

import com.dao.AppDao;
import com.model.Event;
import com.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/event")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class EventServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");
    private static final Set<String> CATEGORIES = Set.of(
            "Technical", "Cultural", "Sports", "Workshop", "Seminar");
    private static final Set<String> DEPARTMENTS = Set.of(
            "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE", "MECH", "CIVIL", "ASE");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String eventId = request.getParameter("id");
        User user = (User) request.getSession().getAttribute("user");

        if (eventId == null || eventId.isBlank()) {
            if (!"ORGANIZER".equals(user.role())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            request.getRequestDispatcher("/WEB-INF/views/event-form.jsp")
                    .forward(request, response);
            return;
        }

        try {
            Event event = new AppDao().event(Integer.parseInt(eventId));
            if (event == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Event not found.");
                return;
            }

            request.setAttribute("event", event);
            request.setAttribute("backPath", participantBackPath(request));
            request.getRequestDispatcher("/WEB-INF/views/event-form.jsp")
                    .forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event ID.");
        } catch (Exception exception) {
            throw new ServletException("Unable to load event.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            requireOrganizer(request);
            AppDao appDao = new AppDao();

            if ("delete".equals(request.getParameter("action"))) {
                appDao.deleteEvent(Integer.parseInt(request.getParameter("id")));
            } else {
                User organizer = (User) request.getSession().getAttribute("user");
                String eventId = request.getParameter("id");
                int id = eventId == null || eventId.isBlank()
                        ? 0
                        : Integer.parseInt(eventId);
                LocalDateTime eventDateTime = dateTime(
                        required(request, "eventDate", "Event date"),
                        required(request, "eventTime", "Event start time"));
                LocalDateTime registrationClosesAt = dateTime(
                        required(
                                request,
                                "registrationCloseDate",
                                "Registration closing date"),
                        required(
                                request,
                                "registrationCloseTime",
                                "Registration closing time"));

                validateDates(eventDateTime, registrationClosesAt);

                Part imagePart = request.getPart("eventImage");
                byte[] imageData = readImage(imagePart);
                if (id == 0 && imageData == null) {
                    throw new IllegalArgumentException("Event image is required.");
                }
                String imageType = imageData == null ? null : imagePart.getContentType();
                boolean hasImage = imageData != null;

                if (!hasImage && id > 0) {
                    Event existingEvent = appDao.event(id);
                    hasImage = existingEvent != null && existingEvent.hasImage();
                }

                int capacity = Integer.parseInt(required(
                        request,
                        "capacity",
                        "Capacity"));
                double fee = Double.parseDouble(required(request, "fee", "Fee"));
                String category = required(request, "category", "Category");
                String department = required(request, "department", "Department");

                if (capacity < 1 || !Double.isFinite(fee) || fee < 0 || fee > 99_999_999.99) {
                    throw new IllegalArgumentException(
                            "Capacity must be at least 1 and fee must be between 0 and 99999999.99.");
                }
                Event existing = id == 0 ? null : appDao.event(id);
                if (id > 0 && existing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Event not found.");
                    return;
                }
                if (existing != null && capacity < existing.registeredCount()) {
                    throw new IllegalArgumentException(
                            "Capacity cannot be less than the registered participant count.");
                }
                if (!CATEGORIES.contains(category)) {
                    throw new IllegalArgumentException("Select a valid event category.");
                }
                if (!DEPARTMENTS.contains(department)) {
                    throw new IllegalArgumentException("Select a valid department.");
                }

                Event event = new Event(
                        id,
                        required(request, "title", "Event title"),
                        required(request, "description", "Description"),
                        category,
                        department,
                        required(request, "venue", "Venue"),
                        eventDateTime,
                        registrationClosesAt,
                        capacity,
                        fee,
                        hasImage,
                        organizer.rollNo(),
                        request.getParameter("active") != null,
                        0);

                appDao.saveEvent(event, imageData, imageType);
            }

            response.sendRedirect(request.getContextPath() + "/events");
        } catch (SecurityException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (DateTimeParseException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event date or time.");
        } catch (IllegalStateException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Event image must not exceed 5 MB.");
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event data.");
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            throw new ServletException("Unable to save event.", exception);
        }
    }

    private String participantBackPath(HttpServletRequest request) {
        String source = request.getParameter("from");
        return "registrations".equals(source) ? "registrations" : "events";
    }

    private String required(
            HttpServletRequest request,
            String parameter,
            String label) {

        String value = request.getParameter(parameter);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (("title".equals(parameter) || "venue".equals(parameter))
                && value.trim().length() > 150) {
            throw new IllegalArgumentException(label + " must not exceed 150 characters.");
        }
        return value.trim();
    }

    private LocalDateTime dateTime(String date, String time) {
        return LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
    }

    private void validateDates(
            LocalDateTime eventDateTime,
            LocalDateTime registrationClosesAt) {

        if (!registrationClosesAt.isBefore(eventDateTime)) {
            throw new IllegalArgumentException(
                    "Registration closing time must be before the event starts.");
        }

        if (registrationClosesAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Registration closing date and time must be in the future.");
        }
    }

    private byte[] readImage(Part imagePart) throws IOException, ServletException {
        if (imagePart == null || imagePart.getSize() == 0) {
            return null;
        }
        if (!ALLOWED_IMAGE_TYPES.contains(imagePart.getContentType())) {
            throw new IllegalArgumentException("Event image must be JPG, PNG, or WEBP.");
        }

        try (InputStream input = imagePart.getInputStream()) {
            return input.readAllBytes();
        }
    }

    private void requireOrganizer(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (!"ORGANIZER".equals(user.role())) {
            throw new SecurityException("Organizer access required.");
        }
    }
}
