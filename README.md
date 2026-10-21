# Campus Event Management System

A role-based, stateful campus event management web application built with JSP, Java Servlets, JDBC, MySQL, HTML, CSS, and JavaScript. It supports participant registration, email OTP verification, organizer OTP login, event CRUD operations, event registration, unique pass generation, profile photos, event images, and searchable registration reports.

## Current Status

- The Maven project builds successfully as a deployable WAR.
- The application uses Java 17, `javax.servlet`, and Apache Tomcat 9.
- MySQL tables and the default organizer are initialized automatically at startup.
- Participant and organizer workflows are implemented with session-based access control.
- Passwords are stored with BCrypt using cost factor 12.
- Gmail SMTP sends OTP and registration confirmation emails.
- Event cancellation and participant feedback are intentionally not included.

## Technology Stack

| Layer | Technology |
| --- | --- |
| Frontend | HTML5, CSS3, JavaScript |
| View | JSP |
| Controller | Java Servlets |
| Authentication | `HttpSession`, `AuthFilter`, and role checks |
| Password security | BCrypt with cost factor 12 |
| Data access | JDBC with prepared statements |
| Database | MySQL 8 or later |
| Email | JavaMail with Gmail SMTP and TLS |
| Runtime | Java 17 and Apache Tomcat 9 |
| Build | Maven WAR |

## User Roles

### Participant

- Registers through the public participant registration page.
- Verifies the entered email address using an OTP before Submit is enabled.
- Uses the unique roll number as the login username.
- Logs in with roll number and password.
- Views counts for available and registered events.
- Browses all available events, including events already registered for.
- Filters available and registered events by category and department.
- Views complete descriptions on the event details page.
- Registers for eligible events and is redirected to Registered Events after success.
- Opens participant, payment, and unique pass details in a dialog.
- Views a complete profile with the uploaded photograph.

### Organizer

- Cannot register through the participant registration page.
- Uses an organizer account created during database initialization.
- Logs in with organizer ID and password, followed by email OTP verification.
- Views total-event, active-event, and registration counts.
- Views and filters all active and inactive events by category and department.
- Creates, views, edits, and deletes events.
- Uploads event images instead of entering image URLs.
- Views member details and registration counts for every event.
- Searches and filters reports by event, college, department, year, and participant information.
- Views the organizer profile.

## Participant Registration

All participant fields are mandatory:

- Full name
- Roll number
- Email address
- Phone number
- College name
- Department
- Study year
- Gender
- Address
- Profile photograph
- Password and confirm password

Roll number, email address, and phone number must be unique. Duplicate values produce an already-exists message.

Password requirements:

- 6 to 15 characters
- At least one lowercase letter
- At least one uppercase letter
- At least one number
- At least one special character

The password fields appear together above the Show password checkbox. Registration submission is enabled only after successful email OTP verification.

### Supported Departments

`CSE`, `IT`, `AI&DS`, `CSM`, `EEE`, `ECE`, `MECH`, `CIVIL`, and `ASE`.

### Supported Colleges

- Lakireddy Bali Reddy college of Engineering
- Bapatla Engineering college
- SRK Institute of Technology
- Andhra Loyola Institute of Engineering and Technology
- Velagapudi Ramakrishna Siddhartha Engineering College
- PSCMR College of Engineering and Technology
- Prasad V. Potluri Siddhartha Institute Of Technology
- RK COLLEGE OF ENGINEERING
- MIC College of Technology
- Amrita Sai Institute of Science and Technology

Study year is limited to years 1 through 4.

## Event Management

An event stores its title, description, category, department, venue, event date and time, registration deadline, capacity, fee, image, organizer, active status, creation time, and registration count.

Supported categories are Technical, Cultural, Sports, Workshop, and Seminar.

All fields are mandatory when creating an event. The registration deadline must be in the future and earlier than the event start. Images must be JPG, PNG, or WEBP and no larger than 5 MB.

Descriptions appear only after selecting View. Back returns to the relevant event list instead of the dashboard. Organizer details return to All Events; participant details return to Available Events or Registered Events according to the source page.

## Event Registration and Passes

The application confirms that the authenticated user is a participant and that the event exists, is active, is still open, has capacity, and has not already been registered for by that participant.

The mini-project simulates completed payment by storing `PAID`. A successful registration creates a unique pass ID, sends a professional HTML confirmation email, and redirects to Registered Events.

## Database

The default database is `event`. `AppDao` creates and maintains these tables:

### `users`

| Column | Purpose |
| --- | --- |
| `roll_no` | Primary key and login username |
| `full_name` | Participant or organizer name |
| `email` | Unique email address |
| `phone_number` | Unique phone number |
| `college_name` | Selected college |
| `password_hash` | BCrypt password hash |
| `role` | `PARTICIPANT` or `ORGANIZER` |
| `department` | Academic department |
| `study_year` | Year from 1 through 4 |
| `gender` | Selected gender |
| `address` | Participant address |
| `profile_photo` | Profile image BLOB |
| `profile_photo_type` | Profile image MIME type |
| `created_at` | Account creation timestamp |

### `events`

| Column | Purpose |
| --- | --- |
| `id` | Auto-increment primary key |
| `title` | Event title |
| `description` | Complete event description |
| `category` | Event category |
| `department` | Responsible department |
| `venue` | Event venue |
| `event_date` | Event date and start time |
| `registration_closes_at` | Registration deadline |
| `capacity` | Maximum registrations |
| `fee` | Registration fee |
| `image_data` | Event image BLOB |
| `image_type` | Event image MIME type |
| `organizer_id` | Organizer foreign key |
| `active` | Availability status |
| `created_at` | Event creation timestamp |

### `registrations`

| Column | Purpose |
| --- | --- |
| `id` | Auto-increment primary key |
| `event_id` | Event foreign key |
| `participant_id` | Participant foreign key |
| `pass_id` | Unique event pass ID |
| `payment_status` | `PAID` or `PENDING` |
| `registered_at` | Registration timestamp |

The `(event_id, participant_id)` pair is unique. Deleting an event cascades to its registration records.

## Session and Security

The application is stateful:

- A successful login stores the authenticated `User` in `HttpSession`.
- Organizer authentication remains pending until OTP verification succeeds.
- `AuthFilter` protects dashboards, events, registrations, profiles, reports, images, and logout.
- Servlets perform participant and organizer role checks.
- Logout invalidates the session.
- Protected responses disable browser caching.
- JDBC operations use prepared statements.
- Passwords use BCrypt cost factor 12.

## Email Workflows

Professional HTML email messages are sent for:

- Participant registration email OTP
- Participant account creation confirmation
- Organizer login OTP
- Event registration confirmation with payment and unique pass details

Current SMTP defaults are Gmail at `smtp.gmail.com:587`, TLS enabled, with user `nitwg6@gmail.com`.

The classroom Gmail app password is embedded in `EmailService` but is intentionally not repeated here. Remove or rotate it before sharing source code or the WAR, and move it to protected secret configuration before production use.

## URL Endpoints

All endpoint paths below follow this application base URL:

```text
http://localhost:<tomcat-port>/campus-events
```

Example login URL when Tomcat uses port `8082`:

```text
http://localhost:8082/campus-events/login
```

### Authentication and Account Endpoints

- **GET `/`** — Public. Opens the application welcome URL, which resolves to login.

- **GET `/login`** — Public. Displays the login form; signed-in users go to their dashboard.

- **POST `/login`** — Public. Checks the username and password; starts organizer email OTP when required.

- **GET `/verify-otp`** — Pending organizer login. Displays the OTP verification form.

- **POST `/verify-otp`** — Pending organizer login. Verifies the OTP and authenticates the organizer session.

- **GET `/register`** — Public. Displays the participant registration form.

- **POST `/register`** — Verified registration email required. Validates and creates a participant account.

- **POST `/registration-email-otp`** — Public registration session. Sends or verifies the email OTP and returns a JSON status message.

- **POST `/logout`** — Signed-in users. Invalidates the session and returns to login.

### Dashboard and Event Endpoints

- **GET `/dashboard`** — Participant or organizer. Displays the summary dashboard for the signed-in role.

- **GET `/events`** — Participant or organizer. Displays Available Events or All Events with category and department filters.

- **GET `/registrations`** — Participant. Displays registered events and pass details with filters. Organizer requests redirect to `/reports`.

- **GET `/profile`** — Participant or organizer. Displays the signed-in user's profile.

- **GET `/event`** — Organizer. Opens the new-event form when no event ID is supplied.

- **GET `/event?id=<eventId>`** — Participant or organizer. Displays event details or the organizer edit form.

- **POST `/event`** — Organizer. Creates, updates, or deletes an event.

- **POST `/registration`** — Participant. Registers for an event, generates a pass, and redirects successful registration to `/registrations`.

- **GET `/reports`** — Organizer. Displays searchable registration records with event, college, department, and year filters.

### Image and Static Asset Endpoints

- **GET `/event-image?id=<eventId>`** — Participant or organizer. Streams the selected event image from MySQL.

- **GET `/profile-photo`** — Participant or organizer. Streams the signed-in user's profile photo when available.

- **GET `/profile-photo?rollNo=<rollNumber>`** — Photo owner or organizer. Streams the requested participant photo after checking access.

- **GET `/assets/style.css`** — Public. Loads the application stylesheet.

- **GET `/assets/validation.js`** — Public. Loads form validation, OTP interaction, and password controls.

### Filter and Action Parameters

- **Event filters — GET `/events`**

  Parameters: `category`, `department`. Applies either or both dropdown filters.

- **Registered event filters — GET `/registrations`**

  Parameters: `category`, `department`. Filters the participant's registered events.

- **Report filters — GET `/reports`**

  Parameters: `search`, `eventId`, `collegeName`, `department`, `year`. Combines text search with report filters.

- **Back to Available Events — GET `/event?id=<eventId>&from=events`**

  Opens event details with Back pointing to Available Events.

- **Back to Registered Events — GET `/event?id=<eventId>&from=registrations`**

  Opens event details with Back pointing to Registered Events.

- **Save an event — POST `/event`**

  Parameters: event form fields and optional `id`. Creates an event when `id` is empty; otherwise updates it.

- **Delete an event — POST `/event`**

  Parameters: `action=delete`, `id`. Deletes the event and its registrations.

- **Register for an event — POST `/registration`**

  Parameter: `eventId`. Submits the selected event for participant registration.

- **Send registration OTP — POST `/registration-email-otp`**

  Parameters: `action=send`, `email`. Sends an OTP valid for five minutes.

- **Verify registration OTP — POST `/registration-email-otp`**

  Parameters: `action=verify`, `email`, `otp`. Verifies the OTP in the same browser session.

Example filtered URLs:

```text
http://localhost:8082/campus-events/events?category=Technical&department=CSE
http://localhost:8082/campus-events/registrations?category=Sports
http://localhost:8082/campus-events/reports?department=IT&year=2
```

GET endpoints can be opened in the browser. POST endpoints are submitted through application forms or JavaScript requests. JSP templates under `WEB-INF/views` are internal views and cannot be opened directly as public URLs.

## Requirements

- JDK 17 or later
- Maven 3.9 or later
- MySQL 8 or later
- Apache Tomcat 9
- Gmail SMTP access for live email delivery

## Configuration

| Variable | Development default |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/event?createDatabaseIfNotExist=true&serverTimezone=Asia/Kolkata` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | `root123` |
| `DEFAULT_ORGANIZER_ID` | `ORG001` |
| `DEFAULT_ORGANIZER_EMAIL` | Application seed default |
| `DEFAULT_ORGANIZER_PASSWORD` | `Admin@123` |
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USER` | `nitwg6@gmail.com` |
| `SMTP_FROM` | Same as `SMTP_USER` |

These defaults are for local classroom development. Change database and organizer credentials before shared deployment.

## Build and Deploy

1. Start MySQL and confirm the configured account can create or use the `event` database.
2. Configure database, organizer, and SMTP values in the Tomcat or Eclipse runtime.
3. Build the WAR:

   ```bash
   mvn package
   ```

4. Deploy `target/campus-events.war` to Apache Tomcat 9.
5. Start Tomcat and open:

   ```text
   http://localhost:<tomcat-port>/campus-events/
   ```

Tomcat commonly uses port `8080`, but Eclipse may use `8081`, `8082`, or another configured port. Use the HTTP port shown in Eclipse Servers and ensure no other process is using it.

`AppContextListener` calls `AppDao.initialize()` at startup. The schema can also be initialized manually by running `com.dao.DatabaseSetup` with the compiled classes and MySQL connector on the classpath.

## Development Accounts and Seed Data

Default organizer development credentials:

- Organizer ID: `ORG001`
- Password: `Admin@123`

The current development database includes demonstration events and ten dummy participants. Participant IDs are `DUMMY001` through `DUMMY010`, and the classroom password is `Password@123`, stored as BCrypt hashes. Some participants are registered for one event and others for multiple events.

Do not retain development accounts or passwords in production.

## Build Output and Documentation

- WAR: `target/campus-events.war`
- Exploded application: `target/campus-events/`
- Architecture image: `docs/campus-event-architecture-a4.png`
- Complete description: `docs/Campus Event Management System Complete Description.docx`

The `target/` and `build/` directories are retained because the Eclipse and Tomcat setup may depend on generated and deployed artifacts.

## Project Structure

The tree below lists the application source, configuration, assets, documentation, and current build folders. Short descriptions appear beside each file. Generated class files, dependency JARs, and individual document preview images are grouped because they are build outputs rather than separate source files.

```text
Event/
|-- .classpath                                  # Eclipse source folders, Java runtime, and dependencies
|-- .project                                    # Eclipse project name and build natures
|-- pom.xml                                     # Maven dependencies, Java version, and WAR packaging
|-- README.md                                   # Project overview, setup, features, and file structure
|
|-- .settings/                                  # Eclipse project and deployment configuration
|   |-- .jsdtscope                              # JavaScript source and library configuration
|   |-- org.eclipse.core.resources.prefs        # Project resource and encoding preferences
|   |-- org.eclipse.jdt.core.prefs              # Java compiler and language settings
|   |-- org.eclipse.m2e.core.prefs              # Maven integration preferences
|   |-- org.eclipse.wst.common.component        # Web deployment mappings and context root
|   |-- org.eclipse.wst.common.project.facet.core.xml
|   |                                           # Java, web, and Tomcat project facets
|   |-- org.eclipse.wst.jsdt.ui.superType.container
|   |                                           # JavaScript browser environment container
|   |-- org.eclipse.wst.jsdt.ui.superType.name   # JavaScript browser environment type
|   `-- org.eclipse.wst.validation.prefs         # Eclipse validation preferences
|
|-- src/
|   |-- main/
|   |   |-- java/com/
|   |   |   |-- dao/                            # Database connection and JDBC operations
|   |   |   |   |-- AppDao.java                 # Schema setup, user/event CRUD, registrations, and reports
|   |   |   |   |-- Dao.java                    # JDBC connections and environment configuration
|   |   |   |   `-- DatabaseSetup.java          # Command-line database initialization entry point
|   |   |   |
|   |   |   |-- model/                          # Account, event, and registration data objects
|   |   |   |   |-- Event.java                  # Event details, availability, and registration count
|   |   |   |   |-- ParticipantEventRegistration.java
|   |   |   |   |                               # Participant event with pass and payment details
|   |   |   |   |-- RegistrationRecord.java     # Participant information for organizer report rows
|   |   |   |   `-- User.java                   # Account identity, role, and participant profile data
|   |   |   |
|   |   |   |-- service/
|   |   |   |   `-- EmailService.java           # SMTP OTP, welcome, and registration receipt emails
|   |   |   |
|   |   |   |-- servlets/                       # HTTP controllers, authentication, and image responses
|   |   |   |   |-- AppContextListener.java     # Database startup setup and shutdown resource cleanup
|   |   |   |   |-- AuthFilter.java             # Authenticated route protection and cache-control headers
|   |   |   |   |-- DashboardServlet.java       # Role dashboards, profiles, and filtered event lists
|   |   |   |   |-- EventImageServlet.java      # Streams event images stored in MySQL
|   |   |   |   |-- EventServlet.java           # Event details and organizer create, edit, and delete
|   |   |   |   |-- Login.java                  # Credential checks and participant/organizer login flow
|   |   |   |   |-- LogoutServlet.java          # Session invalidation and logout redirect
|   |   |   |   |-- OtpServlet.java             # Organizer login OTP verification
|   |   |   |   |-- ProfilePhotoServlet.java    # Streams authorized participant profile photos
|   |   |   |   |-- RegisterServlet.java        # Participant validation and account creation
|   |   |   |   |-- RegistrationEmailOtpServlet.java
|   |   |   |   |                               # Sends and verifies participant registration email OTP
|   |   |   |   |-- RegistrationServlet.java    # Event registration, receipt, and registered-events redirect
|   |   |   |   `-- ReportServlet.java          # Organizer registration search and report filters
|   |   |   |
|   |   |   `-- util/
|   |   |       `-- PasswordUtil.java           # BCrypt password hashing and checking with cost 12
|   |   |
|   |   `-- webapp/                             # Browser assets and server-rendered views
|   |       |-- assets/
|   |       |   |-- style.css                   # Responsive layout, forms, sidebars, cards, and dialogs
|   |       |   `-- validation.js               # Browser validation, OTP interaction, and password controls
|   |       |-- META-INF/
|   |       |   `-- MANIFEST.MF                 # Web application archive manifest
|   |       `-- WEB-INF/
|   |           |-- web.xml                     # Welcome route, session timeout, and cookie settings
|   |           `-- views/                      # Protected JSP templates accessed through Servlets
|   |               |-- event-form.jsp          # Organizer event form and participant event details
|   |               |-- login.jsp               # Login form with password visibility and status messages
|   |               |-- organizerdashboard.jsp  # Organizer summaries, events, reports, and profile
|   |               |-- register.jsp            # Participant form, photo upload, and email verification
|   |               |-- userdashboard.jsp       # Participant summaries, filtered events, passes, and profile
|   |               `-- verify-otp.jsp          # Organizer email OTP verification form
|   `-- test/                                   # Test source folders when generated or created by the IDE
|
|-- docs/                                       # Application diagrams and Word reports
|   |-- campus-event-architecture-a4.png         # A4 architecture diagram image
|   |-- Campus Event Management System Complete Description.docx
|   |                                           # Detailed application description document
|   `-- Campus Event Management System Project Report.docx
|                                               # Template-based report with Java listings and screenshot space
|
|-- build/
|   `-- classes/                                # Retained Eclipse/legacy compiled output
|
`-- target/                                     # Maven build and deployment output
    |-- campus-events.war                       # Deployable application archive for Tomcat 9
    |-- campus-events/                          # Exploded WAR with views, assets, classes, and dependency JARs
    |-- classes/                                # Compiled application Java classes
    |-- jspc/                                   # Generated JSP compilation and verification output
    |-- m2e-wtp/                                # Eclipse Maven web deployment resources
    |-- maven-archiver/                          # Maven archive metadata
    `-- maven-status/                            # Maven compiler execution metadata
```

`src/main/resources` and `src/test/resources` are also configured as optional source paths in `.classpath`; they do not currently contain application configuration files. Empty `target/generated-sources`, `target/generated-test-sources`, and `target/test-classes` folders were removed; Maven or Eclipse can recreate them when needed. Generated folders may vary after rebuilding. Keep the application source in `src/` and use `target/campus-events.war` for deployment.

## Known Limitations

- Payment is simulated; no payment gateway is connected.
- There is no public organizer registration.
- Event cancellation and participant feedback are not included.
- Password recovery is not implemented.
- SMTP secret management requires production hardening.
- Images are stored in MySQL rather than external object storage.
- Automated Maven test coverage is not yet included.

## Verification

The current source compiles and packages successfully with Maven. Participant and organizer workflows, unique constraints, filters, event navigation, session protection, BCrypt hashing, OTP handling, database initialization, event registration redirect, and project documentation have been reviewed against the current implementation.
