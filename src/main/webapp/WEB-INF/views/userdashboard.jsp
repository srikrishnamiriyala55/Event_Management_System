<%@ page import="java.util.*,com.model.*" contentType="text/html;charset=UTF-8" %>
<%!
    private String display(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
%>
<%
    User user = (User) session.getAttribute("user");
    String activeSection = (String) request.getAttribute("activeSection");
    List<Event> availableEvents = (List<Event>) request.getAttribute("availableEvents");
    List<ParticipantEventRegistration> registrations =
            (List<ParticipantEventRegistration>) request.getAttribute("participantRegistrations");
    Set<Integer> registeredIds = new HashSet<>();
    Map<Integer, ParticipantEventRegistration> registrationByEventId = new HashMap<>();
    for (ParticipantEventRegistration registration : registrations) {
        registeredIds.add(registration.event().id());
        registrationByEventId.put(registration.event().id(), registration);
    }
    int availableCount = availableEvents.size();
    String flash = (String) session.getAttribute("flash");
    session.removeAttribute("flash");
    String profileInitial = user.fullName() == null || user.fullName().isBlank()
            ? "?"
            : user.fullName().substring(0, 1).toUpperCase();
%>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Participant Dashboard</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
<header class="topbar participant-topbar">
    <h1>Campus Events</h1>
    <nav>
        <span class="profile-image-wrap topbar-profile-wrap">
            <img class="topbar-profile-photo profile-image" src="profile-photo" alt="Profile photo">
            <span class="profile-image-fallback topbar-profile-fallback" hidden
                    aria-label="Profile photo placeholder"><%=profileInitial%></span>
        </span>
        <span><%=user.fullName()%> · <%=user.rollNo()%></span>
        <form class="inline" action="logout" method="post">
            <button class="btn secondary">Logout</button>
        </form>
    </nav>
</header>

<div class="participant-layout">
    <aside class="sidebar" aria-label="Participant menu">
        <div class="sidebar-title">Participant Menu</div>
        <nav>
            <a class="side-link <%= "dashboard".equals(activeSection) ? "active" : "" %>"
                    href="dashboard">Dashboard</a>
            <a class="side-link <%= "events".equals(activeSection) ? "active" : "" %>"
                    href="events">
                Available Events
            </a>
            <a class="side-link <%= "registrations".equals(activeSection) ? "active" : "" %>"
                    href="registrations">
                Registered Events
            </a>
            <a class="side-link <%= "profile".equals(activeSection) ? "active" : "" %>"
                    href="profile">Profile</a>
        </nav>
    </aside>

    <main class="participant-main">
        <% if (flash != null) { %>
            <div class="message"><%=flash%></div>
        <% } %>

        <section class="dashboard-panel" <%= "dashboard".equals(activeSection) ? "" : "hidden" %>>
            <div class="page-heading">
                <h2>Dashboard</h2>
                <p class="muted">Welcome, <%=user.fullName()%>.</p>
            </div>
            <div class="summary-cards">
                <article class="summary-card">
                    <div class="summary-number"><%=availableCount%></div>
                    <h3>Available events</h3>
                    <p>Events currently open for your registration.</p>
                    <a href="events">Click here</a>
                </article>
                <article class="summary-card registered-summary">
                    <div class="summary-number"><%=registrations.size()%></div>
                    <h3>Registered events</h3>
                    <p>Events for which your registration is complete.</p>
                    <a href="registrations">Click here</a>
                </article>
            </div>
        </section>

        <section class="dashboard-panel" <%= "events".equals(activeSection) ? "" : "hidden" %>>
            <div class="page-heading">
                <h2>Available Events</h2>
                <p class="muted">Browse events currently open for registration.</p>
            </div>
            <form class="filter event-filter" action="events" method="get">
                <div>
                    <label for="availableCategory">Category</label>
                    <select id="availableCategory" name="category">
                        <option value="">All categories</option>
                        <% for (String category : new String[]{
                                "Technical", "Cultural", "Sports",
                                "Workshop", "Seminar"}) { %>
                            <option value="<%=category%>"
                                    <%=category.equals(request.getParameter("category"))
                                            ? "selected" : ""%>>
                                <%=category%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="availableDepartment">Department</label>
                    <select id="availableDepartment" name="department">
                        <option value="">All departments</option>
                        <% for (String department : new String[]{
                                "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                                "MECH", "CIVIL", "ASE"}) { %>
                            <option value="<%=department%>"
                                    <%=department.equals(
                                            request.getParameter("department"))
                                                    ? "selected" : ""%>>
                                <%=department%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div class="report-filter-actions">
                    <button class="btn">Apply filters</button>
                    <a class="btn secondary" href="events">Clear</a>
                </div>
            </form>
            <div class="cards">
                <%
                    boolean hasAvailableEvents = false;
                    for (Event event : availableEvents) {
                        hasAvailableEvents = true;
                        ParticipantEventRegistration existingRegistration =
                                registrationByEventId.get(event.id());
                %>
                    <article class="card">
                        <img src="<%=event.hasImage()
                                ? "event-image?id=" + event.id()
                                : "https://placehold.co/640x360/12355b/ffffff?text=Campus+Event"%>"
                                alt="<%=event.title()%>">
                        <div class="card-body">
                            <span class="badge"><%=event.category()%></span>
                            <h3><%=event.title()%></h3>
                            <div class="meta">
                                <%=event.department()%> · <%=event.date().toLocalDate()%><br>
                                <%=event.venue()%><br>
                                Registration closes: <%=event.registrationClosesAt()%><br>
                                Seats: <%=event.registeredCount()%>/<%=event.capacity()%>
                                · Fee: ₹<%=String.format("%.2f", event.fee())%>
                            </div>
                            <div class="actions">
                                <a class="btn secondary"
                                        href="event?id=<%=event.id()%>&from=events">View</a>
                                <% if (existingRegistration == null) { %>
                                    <form action="registration" method="post">
                                        <input type="hidden" name="eventId" value="<%=event.id()%>">
                                        <button class="btn">Register now</button>
                                    </form>
                                <% } else { %>
                                    <button class="btn success registration-details-button" type="button"
                                            data-dialog="available-registration-<%=event.id()%>">
                                        Already Registered
                                    </button>
                                <% } %>
                            </div>
                            <% if (existingRegistration != null) { %>
                                <dialog id="available-registration-<%=event.id()%>"
                                        class="registration-dialog">
                                    <div class="registration-dialog-content">
                                        <h3>Participant and pass details</h3>
                                        <p class="muted"><%=event.title()%></p>
                                        <dl class="details-list compact-details">
                                            <div><dt>Name</dt><dd><%=user.fullName()%></dd></div>
                                            <div><dt>Roll number</dt><dd><%=user.rollNo()%></dd></div>
                                            <div><dt>Email</dt><dd><%=user.email()%></dd></div>
                                            <div>
                                                <dt>Phone</dt>
                                                <dd><%=display(user.phoneNumber())%></dd>
                                            </div>
                                            <div>
                                                <dt>Pass ID</dt>
                                                <dd><strong><%=existingRegistration.passId()%></strong></dd>
                                            </div>
                                            <div>
                                                <dt>Payment</dt>
                                                <dd><%=existingRegistration.paymentStatus()%></dd>
                                            </div>
                                            <div>
                                                <dt>Registered</dt>
                                                <dd><%=existingRegistration.registeredAt()%></dd>
                                            </div>
                                        </dl>
                                        <form method="dialog" class="actions">
                                            <button class="btn secondary" type="submit">
                                                &larr; Back
                                            </button>
                                        </form>
                                    </div>
                                </dialog>
                            <% } %>
                        </div>
                    </article>
                <% } %>
                <% if (!hasAvailableEvents) { %>
                    <div class="empty">
                        No available events match the selected filters.
                    </div>
                <% } %>
            </div>
        </section>

        <section class="dashboard-panel"
                <%= "registrations".equals(activeSection) ? "" : "hidden" %>>
            <div class="page-heading">
                <h2>Registered Events</h2>
                <p class="muted">View event information and your registration pass details.</p>
            </div>
            <form class="filter event-filter" action="registrations" method="get">
                <div>
                    <label for="registeredCategory">Category</label>
                    <select id="registeredCategory" name="category">
                        <option value="">All categories</option>
                        <% for (String category : new String[]{
                                "Technical", "Cultural", "Sports",
                                "Workshop", "Seminar"}) { %>
                            <option value="<%=category%>"
                                    <%=category.equals(request.getParameter("category"))
                                            ? "selected" : ""%>>
                                <%=category%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="registeredDepartment">Department</label>
                    <select id="registeredDepartment" name="department">
                        <option value="">All departments</option>
                        <% for (String department : new String[]{
                                "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                                "MECH", "CIVIL", "ASE"}) { %>
                            <option value="<%=department%>"
                                    <%=department.equals(
                                            request.getParameter("department"))
                                                    ? "selected" : ""%>>
                                <%=department%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div class="report-filter-actions">
                    <button class="btn">Apply filters</button>
                    <a class="btn secondary" href="registrations">Clear</a>
                </div>
            </form>
            <div class="cards">
                <% for (ParticipantEventRegistration registration : registrations) {
                    Event event = registration.event(); %>
                    <article class="card">
                        <img src="<%=event.hasImage()
                                ? "event-image?id=" + event.id()
                                : "https://placehold.co/640x360/207a4b/ffffff?text=Registered"%>"
                                alt="<%=event.title()%>">
                        <div class="card-body">
                            <span class="badge registered-badge">Registered</span>
                            <h3><%=event.title()%></h3>
                            <div class="meta">
                                <%=event.date().toLocalDate()%> · <%=event.venue()%><br>
                                Pass ID: <strong><%=registration.passId()%></strong>
                            </div>
                            <div class="actions">
                                <a class="btn secondary"
                                        href="event?id=<%=event.id()%>&from=registrations">View</a>
                                <button class="btn registration-details-button" type="button"
                                        data-dialog="registered-registration-<%=event.id()%>">
                                    Details
                                </button>
                            </div>
                            <dialog id="registered-registration-<%=event.id()%>"
                                    class="registration-dialog">
                                <div class="registration-dialog-content">
                                    <h3>Participant and pass details</h3>
                                    <p class="muted"><%=event.title()%></p>
                                    <dl class="details-list compact-details">
                                        <div><dt>Name</dt><dd><%=user.fullName()%></dd></div>
                                        <div><dt>Roll number</dt><dd><%=user.rollNo()%></dd></div>
                                        <div><dt>Email</dt><dd><%=user.email()%></dd></div>
                                        <div>
                                            <dt>Phone</dt>
                                            <dd><%=display(user.phoneNumber())%></dd>
                                        </div>
                                        <div>
                                            <dt>Pass ID</dt>
                                            <dd><strong><%=registration.passId()%></strong></dd>
                                        </div>
                                        <div>
                                            <dt>Payment</dt>
                                            <dd><%=registration.paymentStatus()%></dd>
                                        </div>
                                        <div>
                                            <dt>Registered</dt>
                                            <dd><%=registration.registeredAt()%></dd>
                                        </div>
                                    </dl>
                                    <form method="dialog" class="actions">
                                        <button class="btn secondary" type="submit">
                                            &larr; Back
                                        </button>
                                    </form>
                                </div>
                            </dialog>
                        </div>
                    </article>
                <% } %>
                <% if (registrations.isEmpty()) { %>
                    <div class="empty">
                        No registered events match the selected filters.
                    </div>
                <% } %>
            </div>
        </section>

        <section class="dashboard-panel" <%= "profile".equals(activeSection) ? "" : "hidden" %>>
            <div class="page-heading">
                <h2>Participant Profile</h2>
                <p class="muted">Your complete registration information.</p>
            </div>
            <article class="profile-card">
                <div class="profile-photo-column">
                    <span class="profile-image-wrap profile-page-photo-wrap">
                        <img class="profile-image" src="profile-photo"
                                alt="<%=user.fullName()%> profile photo">
                        <span class="profile-image-fallback profile-page-photo-fallback" hidden
                                aria-label="Profile photo placeholder"><%=profileInitial%></span>
                    </span>
                    <h3><%=user.fullName()%></h3>
                    <span class="badge"><%=user.rollNo()%></span>
                </div>
                <dl class="details-list">
                    <div><dt>Full name</dt><dd><%=user.fullName()%></dd></div>
                    <div><dt>Roll number</dt><dd><%=user.rollNo()%></dd></div>
                    <div><dt>Email</dt><dd><%=user.email()%></dd></div>
                    <div><dt>Phone number</dt><dd><%=display(user.phoneNumber())%></dd></div>
                    <div><dt>College</dt><dd><%=display(user.collegeName())%></dd></div>
                    <div><dt>Department</dt><dd><%=display(user.department())%></dd></div>
                    <div><dt>Year</dt><dd><%=user.year()%></dd></div>
                    <div><dt>Gender</dt><dd><%=display(user.gender())%></dd></div>
                    <div class="full-detail"><dt>Address</dt><dd><%=display(user.address())%></dd></div>
                </dl>
            </article>
        </section>
    </main>
</div>

<script>
(() => {
    document.querySelectorAll('.profile-image').forEach(image => {
        const showFallback = () => {
            image.hidden = true;
            const fallback = image.nextElementSibling;
            if (fallback) {
                fallback.hidden = false;
            }
        };
        image.addEventListener('error', showFallback);
        if (image.complete && image.naturalWidth === 0) {
            showFallback();
        }
    });

    document.querySelectorAll('.registration-details-button').forEach(button => {
        button.addEventListener('click', () => {
            const dialog = document.getElementById(button.dataset.dialog);
            dialog.showModal();
        });
    });
})();
</script>
</body>
</html>
