package com.servlets;

import com.dao.AppDao;
import com.model.User;
import com.service.EmailService;
import com.util.PasswordUtil;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet("/register")
@MultipartConfig(maxFileSize = 2_097_152)
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Set<String> DEPARTMENTS = Set.of(
            "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE", "MECH", "CIVIL", "ASE");
    private static final Set<String> COLLEGES = Set.of(
            "Lakireddy Bali Reddy college of Engineering",
            "Bapatla Engineering college",
            "SRK Institute of Technology",
            "Andhra Loyola Institute of Engineering and Technology",
            "Velagapudi Ramakrishna Siddhartha Engineering College",
            "PSCMR College of Engineering and Technology",
            "Prasad V. Potluri Siddhartha Institute Of Technology",
            "RK COLLEGE OF ENGINEERING",
            "MIC College of Technology",
            "Amrita Sai Institute of Science and Technology");
    private static final Set<String> GENDERS = Set.of("Male", "Female", "Other");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/register.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String rollNumber = normalize(request.getParameter("rollNo")).toUpperCase();
        String email = normalize(request.getParameter("email")).toLowerCase();
        String password = normalize(request.getParameter("password"));

        HttpSession session = request.getSession(false);
        String verifiedEmail = session == null
                ? null
                : (String) session.getAttribute("verifiedRegistrationEmail");

        if (verifiedEmail == null || !verifiedEmail.equals(email)) {
            request.setAttribute(
                    "errorMessage",
                    "Verify this email address with OTP before submitting.");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp")
                    .forward(request, response);
            return;
        }

        AppDao appDao = new AppDao();
        boolean participantCreated = false;

        try {
            validateRegistration(request, rollNumber, password);

            Part profilePhotoPart = request.getPart("profilePhoto");
            validateProfilePhoto(profilePhotoPart);

            String conflict = appDao.registrationConflict(
                    rollNumber,
                    email,
                    normalize(request.getParameter("phoneNumber")));
            if (conflict != null) {
                throw new IllegalArgumentException(conflict);
            }

            byte[] profilePhoto = profilePhotoPart.getInputStream().readAllBytes();
            User participant = buildParticipant(request, rollNumber, email);

            appDao.createParticipant(
                    participant,
                    PasswordUtil.hash(password),
                    profilePhoto,
                    profilePhotoPart.getContentType());

            participantCreated = true;
            session.removeAttribute("verifiedRegistrationEmail");

            sendRegistrationConfirmation(participant);

            response.sendRedirect(
                    request.getContextPath() + "/login?status=registered");
        } catch (Exception exception) {
            if (participantCreated) {
                removeIncompleteParticipant(appDao, rollNumber);
            }

            getServletContext().log("Participant registration failed", exception);
            request.setAttribute("errorMessage", registrationErrorMessage(exception));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp")
                    .forward(request, response);
        }
    }

    private void validateRegistration(
            HttpServletRequest request,
            String rollNumber,
            String password) {

        String confirmPassword = requireText(
                request,
                "confirmPassword",
                "Confirm password");

        requireText(request, "fullName", "Full name");
        requireText(request, "rollNo", "Roll number");
        requireText(request, "email", "Email");
        requireText(request, "phoneNumber", "Phone number");
        requireText(request, "collegeName", "College name");
        requireText(request, "department", "Department");
        requireText(request, "year", "Year");
        requireText(request, "gender", "Gender");
        requireText(request, "address", "Address");

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (!rollNumber.matches("[A-Z0-9]{5,20}")) {
            throw new IllegalArgumentException(
                    "Roll number must contain 5 to 20 letters or numbers.");
        }

        String phoneNumber = normalize(request.getParameter("phoneNumber"));
        if (!phoneNumber.matches("[6-9][0-9]{9}")) {
            throw new IllegalArgumentException("Enter a valid 10-digit mobile number.");
        }

        String collegeName = normalize(request.getParameter("collegeName"));
        if (!COLLEGES.contains(collegeName)) {
            throw new IllegalArgumentException("Select a valid college name.");
        }

        String department = normalize(request.getParameter("department"));
        if (!DEPARTMENTS.contains(department)) {
            throw new IllegalArgumentException("Select a valid department.");
        }

        String gender = normalize(request.getParameter("gender"));
        if (!GENDERS.contains(gender)) {
            throw new IllegalArgumentException("Select a valid gender.");
        }

        int year;
        try {
            year = Integer.parseInt(normalize(request.getParameter("year")));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Select a valid study year.");
        }
        if (year < 1 || year > 4) {
            throw new IllegalArgumentException("Select a valid study year.");
        }

        String strongPasswordPattern =
                "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)"
                        + "(?=.*[^A-Za-z0-9]).{6,15}$";

        if (!password.matches(strongPasswordPattern)) {
            throw new IllegalArgumentException(
                    "Password must be 6 to 15 characters and contain at least one "
                            + "lowercase letter, uppercase letter, number and special symbol.");
        }
    }

    private String requireText(
            HttpServletRequest request,
            String parameter,
            String label) {

        String value = request.getParameter(parameter);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateProfilePhoto(Part profilePhotoPart) {
        if (profilePhotoPart == null || profilePhotoPart.getSize() == 0) {
            throw new IllegalArgumentException("Profile photo is required.");
        }

        String contentType = profilePhotoPart.getContentType();
        boolean supportedType = contentType != null
                && contentType.matches("image/(jpeg|png|webp)");

        if (!supportedType) {
            throw new IllegalArgumentException(
                    "Upload a valid JPG, PNG or WEBP profile photo.");
        }
    }

    private User buildParticipant(
            HttpServletRequest request,
            String rollNumber,
            String email) {

        return new User(
                rollNumber,
                request.getParameter("fullName").trim(),
                email,
                request.getParameter("phoneNumber").trim(),
                request.getParameter("collegeName").trim(),
                null,
                "PARTICIPANT",
                request.getParameter("department").trim(),
                Integer.parseInt(request.getParameter("year")),
                request.getParameter("gender").trim(),
                request.getParameter("address").trim());
    }

    private void sendRegistrationConfirmation(User participant) {
        try {
            EmailService.sendParticipantWelcome(participant);
        } catch (Exception mailException) {
            getServletContext().log(
                    "Registration saved but confirmation email failed",
                    mailException);
        }
    }

    private void removeIncompleteParticipant(AppDao appDao, String rollNumber) {
        try {
            appDao.deleteUser(rollNumber);
        } catch (Exception cleanupException) {
            getServletContext().log(
                    "Unable to remove incomplete participant",
                    cleanupException);
        }
    }

    private String registrationErrorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message != null && message.toLowerCase().contains("duplicate")) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("phone")) {
                return "Phone number already exists.";
            }
            if (lowerMessage.contains("email")) {
                return "Email address already exists.";
            }
            if (lowerMessage.contains("primary") || lowerMessage.contains("roll")) {
                return "Roll number already exists.";
            }
            return "Roll number, email address, or phone number already exists.";
        }

        return message == null ? "Registration failed." : message;
    }
}
