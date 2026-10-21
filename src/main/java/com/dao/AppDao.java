package com.dao;

import com.model.Event;
import com.model.ParticipantEventRegistration;
import com.model.RegistrationRecord;
import com.model.User;
import com.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AppDao {

    private static final String USERS = "users";
    private static final String EVENTS = "events";
    private static final String REGISTRATIONS = "registrations";

    public void initialize() throws SQLException {
        try (Connection connection = Dao.getConnection();
                Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + USERS + " ("
                            + "roll_no VARCHAR(30) PRIMARY KEY,"
                            + "full_name VARCHAR(100) NOT NULL,"
                            + "email VARCHAR(150) NOT NULL UNIQUE,"
                            + "phone_number VARCHAR(15),"
                            + "college_name VARCHAR(150),"
                            + "password_hash VARCHAR(255) NOT NULL,"
                            + "role ENUM('PARTICIPANT','ORGANIZER') NOT NULL,"
                            + "department VARCHAR(10),"
                            + "study_year INT,"
                            + "gender VARCHAR(20),"
                            + "address VARCHAR(500),"
                            + "profile_photo MEDIUMBLOB,"
                            + "profile_photo_type VARCHAR(100),"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT uk_users_phone UNIQUE(phone_number)"
                            + ")");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + EVENTS + " ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "title VARCHAR(150) NOT NULL,"
                            + "description TEXT NOT NULL,"
                            + "category VARCHAR(60) NOT NULL,"
                            + "department VARCHAR(10) NOT NULL,"
                            + "venue VARCHAR(150) NOT NULL,"
                            + "event_date DATETIME NOT NULL,"
                            + "registration_closes_at DATETIME NOT NULL,"
                            + "capacity INT NOT NULL,"
                            + "fee DECIMAL(10,2) DEFAULT 0,"
                            + "image_data MEDIUMBLOB,"
                            + "image_type VARCHAR(100),"
                            + "organizer_id VARCHAR(30) NOT NULL,"
                            + "active BOOLEAN DEFAULT TRUE,"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_cem_event_organizer "
                            + "FOREIGN KEY(organizer_id) REFERENCES " + USERS + "(roll_no)"
                            + ")");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + REGISTRATIONS + " ("
                            + "id INT PRIMARY KEY AUTO_INCREMENT,"
                            + "event_id INT NOT NULL,"
                            + "participant_id VARCHAR(30) NOT NULL,"
                            + "pass_id VARCHAR(40) NOT NULL UNIQUE,"
                            + "payment_status ENUM('PAID','PENDING') DEFAULT 'PAID',"
                            + "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                            + "UNIQUE(event_id, participant_id),"
                            + "CONSTRAINT fk_cem_registration_event "
                            + "FOREIGN KEY(event_id) REFERENCES " + EVENTS
                            + "(id) ON DELETE CASCADE,"
                            + "CONSTRAINT fk_cem_registration_user "
                            + "FOREIGN KEY(participant_id) REFERENCES " + USERS + "(roll_no)"
                            + ")");
        }

        ensureUserConstraints();
        ensureEventColumns();
        seedOrganizer();
    }

    private void ensureUserConstraints() throws SQLException {
        try (Connection connection = Dao.getConnection();
                Statement statement = connection.createStatement()) {

            boolean phoneIndexExists = false;
            try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                    connection.getCatalog(),
                    null,
                    USERS,
                    true,
                    false)) {

                while (indexes.next()) {
                    String columnName = indexes.getString("COLUMN_NAME");
                    if ("phone_number".equalsIgnoreCase(columnName)) {
                        phoneIndexExists = true;
                        break;
                    }
                }
            }

            if (!phoneIndexExists) {
                statement.executeUpdate(
                        "CREATE UNIQUE INDEX uk_users_phone ON "
                                + USERS + "(phone_number)");
            }
        }
    }

    private void ensureEventColumns() throws SQLException {
        try (Connection connection = Dao.getConnection();
                Statement statement = connection.createStatement()) {

            addColumnIfMissing(
                    connection,
                    statement,
                    "department",
                    "VARCHAR(10) NULL");
            addColumnIfMissing(
                    connection,
                    statement,
                    "registration_closes_at",
                    "DATETIME NULL");
            addColumnIfMissing(
                    connection,
                    statement,
                    "image_data",
                    "MEDIUMBLOB");
            addColumnIfMissing(
                    connection,
                    statement,
                    "image_type",
                    "VARCHAR(100)");

            statement.executeUpdate(
                    "UPDATE " + EVENTS
                            + " SET registration_closes_at = event_date"
                            + " WHERE registration_closes_at IS NULL");
            statement.executeUpdate(
                    "UPDATE " + EVENTS
                            + " SET department = 'CSE'"
                            + " WHERE department IS NULL OR department = ''");
            statement.executeUpdate(
                    "ALTER TABLE " + EVENTS
                            + " MODIFY COLUMN registration_closes_at DATETIME NOT NULL");
            statement.executeUpdate(
                    "ALTER TABLE " + EVENTS
                            + " MODIFY COLUMN department VARCHAR(10) NOT NULL");

            dropColumnIfPresent(connection, statement, "image_url");
        }
    }

    private void addColumnIfMissing(
            Connection connection,
            Statement statement,
            String columnName,
            String definition) throws SQLException {

        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                null,
                EVENTS,
                columnName)) {

            if (!columns.next()) {
                statement.executeUpdate(
                        "ALTER TABLE " + EVENTS
                                + " ADD COLUMN " + columnName + " " + definition);
            }
        }
    }

    private void dropColumnIfPresent(
            Connection connection,
            Statement statement,
            String columnName) throws SQLException {

        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                null,
                EVENTS,
                columnName)) {

            if (columns.next()) {
                statement.executeUpdate(
                        "ALTER TABLE " + EVENTS + " DROP COLUMN " + columnName);
            }
        }
    }

    private void seedOrganizer() throws SQLException {
        String sql = "INSERT IGNORE INTO " + USERS
                + " (roll_no, full_name, email, password_hash, role) "
                + "VALUES (?, ?, ?, ?, 'ORGANIZER')";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, Dao.env("DEFAULT_ORGANIZER_ID", "ORG001"));
            statement.setString(2, "Campus Organizer");
            statement.setString(
                    3,
                    Dao.env("DEFAULT_ORGANIZER_EMAIL", "nitwg6@gmail.com"));
            statement.setString(
                    4,
                    PasswordUtil.hash(
                            Dao.env("DEFAULT_ORGANIZER_PASSWORD", "Admin@123")));
            statement.executeUpdate();
        }
    }

    public User findUser(String rollNumber) throws SQLException {
        String sql = "SELECT * FROM " + USERS + " WHERE roll_no = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }

    public String registrationConflict(
            String rollNumber,
            String email,
            String phoneNumber) throws SQLException {

        String sql = "SELECT "
                + "COALESCE(MAX(roll_no = ?), 0) roll_exists, "
                + "COALESCE(MAX(email = ?), 0) email_exists, "
                + "COALESCE(MAX(phone_number = ?), 0) phone_exists "
                + "FROM " + USERS;

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            statement.setString(2, email);
            statement.setString(3, phoneNumber);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                if (result.getBoolean("roll_exists")) {
                    return "Roll number already exists.";
                }
                if (result.getBoolean("email_exists")) {
                    return "Email address already exists.";
                }
                if (result.getBoolean("phone_exists")) {
                    return "Phone number already exists.";
                }
                return null;
            }
        }
    }

    public void createParticipant(
            User participant,
            String passwordHash,
            byte[] profilePhoto,
            String profilePhotoType) throws SQLException {

        String sql = "INSERT INTO " + USERS
                + " (roll_no, full_name, email, phone_number, college_name,"
                + " password_hash, role, department, study_year, gender, address,"
                + " profile_photo, profile_photo_type) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PARTICIPANT', ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, participant.rollNo());
            statement.setString(2, participant.fullName());
            statement.setString(3, participant.email());
            statement.setString(4, participant.phoneNumber());
            statement.setString(5, participant.collegeName());
            statement.setString(6, passwordHash);
            statement.setString(7, participant.department());
            statement.setInt(8, participant.year());
            statement.setString(9, participant.gender());
            statement.setString(10, participant.address());
            statement.setBytes(11, profilePhoto);
            statement.setString(12, profilePhotoType);
            statement.executeUpdate();
        }
    }

    public void deleteUser(String rollNumber) throws SQLException {
        String sql = "DELETE FROM " + USERS + " WHERE roll_no = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            statement.executeUpdate();
        }
    }

    public List<Event> events(boolean activeOnly) throws SQLException {
        String sql = "SELECT e.*, "
                + "(SELECT COUNT(*) FROM " + REGISTRATIONS
                + " r WHERE r.event_id = e.id) registered_count "
                + "FROM " + EVENTS + " e"
                + (activeOnly
                        ? " WHERE e.active = TRUE"
                                + " AND e.event_date >= NOW()"
                                + " AND e.registration_closes_at >= NOW()"
                        : "")
                + " ORDER BY e.event_date";

        List<Event> events = new ArrayList<>();
        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                events.add(mapEvent(result));
            }
        }
        return events;
    }

    public Event event(int eventId) throws SQLException {
        String sql = "SELECT e.*, "
                + "(SELECT COUNT(*) FROM " + REGISTRATIONS
                + " r WHERE r.event_id = e.id) registered_count "
                + "FROM " + EVENTS + " e WHERE e.id = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapEvent(result) : null;
            }
        }
    }

    public Set<Integer> registeredEventIds(String rollNumber) throws SQLException {
        String sql = "SELECT event_id FROM " + REGISTRATIONS
                + " WHERE participant_id = ?";
        Set<Integer> eventIds = new HashSet<>();

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    eventIds.add(result.getInt("event_id"));
                }
            }
        }
        return eventIds;
    }

    public List<ParticipantEventRegistration> participantRegistrations(String rollNumber)
            throws SQLException {

        String sql = "SELECT e.*, "
                + "(SELECT COUNT(*) FROM " + REGISTRATIONS
                + " counted WHERE counted.event_id = e.id) registered_count, "
                + "r.pass_id, r.payment_status, r.registered_at "
                + "FROM " + REGISTRATIONS + " r "
                + "JOIN " + EVENTS + " e ON e.id = r.event_id "
                + "WHERE r.participant_id = ? "
                + "ORDER BY e.event_date";

        List<ParticipantEventRegistration> registrations = new ArrayList<>();
        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    registrations.add(new ParticipantEventRegistration(
                            mapEvent(result),
                            result.getString("pass_id"),
                            result.getString("payment_status"),
                            result.getTimestamp("registered_at").toLocalDateTime()));
                }
            }
        }
        return registrations;
    }

    public void saveEvent(
            Event event,
            byte[] imageData,
            String imageType) throws SQLException {

        boolean creating = event.id() == 0;
        String sql = creating
                ? "INSERT INTO " + EVENTS
                        + " (title, description, category, department, venue, event_date,"
                        + " registration_closes_at, capacity, fee, image_data, image_type,"
                        + " organizer_id, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "UPDATE " + EVENTS
                        + " SET title = ?, description = ?, category = ?, department = ?,"
                        + " venue = ?, event_date = ?, registration_closes_at = ?, capacity = ?,"
                        + " fee = ?, image_data = COALESCE(?, image_data),"
                        + " image_type = COALESCE(?, image_type),"
                        + " organizer_id = ?, active = ? WHERE id = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, event.title());
            statement.setString(2, event.description());
            statement.setString(3, event.category());
            statement.setString(4, event.department());
            statement.setString(5, event.venue());
            statement.setTimestamp(6, Timestamp.valueOf(event.date()));
            statement.setTimestamp(7, Timestamp.valueOf(event.registrationClosesAt()));
            statement.setInt(8, event.capacity());
            statement.setDouble(9, event.fee());
            statement.setBytes(10, imageData);
            statement.setString(11, imageType);
            statement.setString(12, event.organizerId());
            statement.setBoolean(13, event.active());
            if (!creating) {
                statement.setInt(14, event.id());
            }
            statement.executeUpdate();
        }
    }

    public byte[] eventImage(int eventId) throws SQLException {
        String sql = "SELECT image_data FROM " + EVENTS + " WHERE id = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBytes("image_data") : null;
            }
        }
    }

    public String eventImageType(int eventId) throws SQLException {
        String sql = "SELECT image_type FROM " + EVENTS + " WHERE id = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("image_type") : null;
            }
        }
    }

    public void deleteEvent(int eventId) throws SQLException {
        String sql = "DELETE FROM " + EVENTS + " WHERE id = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, eventId);
            statement.executeUpdate();
        }
    }

    public String register(int eventId, String rollNumber) throws SQLException {
        String passId = "EVT-" + eventId + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try (Connection connection = Dao.getConnection()) {
            connection.setAutoCommit(false);
            try {
                verifySeatAvailability(connection, eventId);
                insertRegistration(connection, eventId, rollNumber, passId);
                connection.commit();
                return passId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void verifySeatAvailability(Connection connection, int eventId)
            throws SQLException {

        String sql = "SELECT capacity, "
                + "(SELECT COUNT(*) FROM " + REGISTRATIONS
                + " WHERE event_id = ?) used "
                + "FROM " + EVENTS
                + " WHERE id = ? AND active = TRUE"
                + " AND registration_closes_at >= NOW() FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, eventId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt("used") >= result.getInt("capacity")) {
                    throw new SQLException("Event is full or unavailable.");
                }
            }
        }
    }

    private void insertRegistration(
            Connection connection,
            int eventId,
            String rollNumber,
            String passId) throws SQLException {

        String sql = "INSERT INTO " + REGISTRATIONS
                + " (event_id, participant_id, pass_id, payment_status) "
                + "VALUES (?, ?, ?, 'PAID')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setString(2, rollNumber);
            statement.setString(3, passId);
            statement.executeUpdate();
        }
    }

    public List<RegistrationRecord> report(
            Integer eventId,
            String collegeName,
            String department,
            Integer year,
            String search) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT r.*, e.title, u.full_name, u.email, u.phone_number,"
                        + " u.college_name, u.department, u.study_year "
                        + "FROM " + REGISTRATIONS + " r "
                        + "JOIN " + EVENTS + " e ON e.id = r.event_id "
                        + "JOIN " + USERS + " u ON u.roll_no = r.participant_id "
                        + "WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();

        appendReportFilters(
                sql,
                parameters,
                eventId,
                collegeName,
                department,
                year,
                search);
        sql.append(" ORDER BY r.registered_at DESC");

        List<RegistrationRecord> records = new ArrayList<>();
        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    records.add(mapRegistrationRecord(result));
                }
            }
        }
        return records;
    }

    private void appendReportFilters(
            StringBuilder sql,
            List<Object> parameters,
            Integer eventId,
            String collegeName,
            String department,
            Integer year,
            String search) {

        if (eventId != null) {
            sql.append(" AND e.id = ?");
            parameters.add(eventId);
        }
        if (collegeName != null && !collegeName.isBlank()) {
            sql.append(" AND u.college_name = ?");
            parameters.add(collegeName);
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND u.department = ?");
            parameters.add(department);
        }
        if (year != null) {
            sql.append(" AND u.study_year = ?");
            parameters.add(year);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.roll_no LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?"
                    + " OR u.phone_number LIKE ? OR r.pass_id LIKE ?)");
            String term = "%" + search.trim() + "%";
            parameters.addAll(List.of(term, term, term, term, term));
        }
    }

    public byte[] profilePhoto(String rollNumber) throws SQLException {
        String sql = "SELECT profile_photo FROM " + USERS + " WHERE roll_no = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBytes("profile_photo") : null;
            }
        }
    }

    public String profilePhotoType(String rollNumber) throws SQLException {
        String sql = "SELECT profile_photo_type FROM " + USERS + " WHERE roll_no = ?";

        try (Connection connection = Dao.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rollNumber);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("profile_photo_type") : null;
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getString("roll_no"),
                result.getString("full_name"),
                result.getString("email"),
                result.getString("phone_number"),
                result.getString("college_name"),
                result.getString("password_hash"),
                result.getString("role"),
                result.getString("department"),
                result.getInt("study_year"),
                result.getString("gender"),
                result.getString("address"));
    }

    private Event mapEvent(ResultSet result) throws SQLException {
        return new Event(
                result.getInt("id"),
                result.getString("title"),
                result.getString("description"),
                result.getString("category"),
                result.getString("department"),
                result.getString("venue"),
                result.getTimestamp("event_date").toLocalDateTime(),
                result.getTimestamp("registration_closes_at").toLocalDateTime(),
                result.getInt("capacity"),
                result.getDouble("fee"),
                result.getBytes("image_data") != null,
                result.getString("organizer_id"),
                result.getBoolean("active"),
                result.getInt("registered_count"));
    }

    private RegistrationRecord mapRegistrationRecord(ResultSet result)
            throws SQLException {

        return new RegistrationRecord(
                result.getInt("event_id"),
                result.getString("title"),
                result.getString("participant_id"),
                result.getString("full_name"),
                result.getString("email"),
                result.getString("phone_number"),
                result.getString("college_name"),
                result.getString("department"),
                result.getInt("study_year"),
                result.getString("pass_id"),
                result.getString("payment_status"),
                result.getTimestamp("registered_at").toLocalDateTime());
    }
}
