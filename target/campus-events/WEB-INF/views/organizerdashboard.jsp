<%@ page import="com.util.Html" %>
<%@ page import="java.util.*,com.model.*" contentType="text/html;charset=UTF-8" %>
<%
    User user = (User) session.getAttribute("user");
    String activeSection = (String) request.getAttribute("activeSection");
    List<Event> events = (List<Event>) request.getAttribute("events");
    List<RegistrationRecord> records =
            (List<RegistrationRecord>) request.getAttribute("records");
    if (records == null) {
        records = Collections.emptyList();
    }
    String[] colleges = {
        "Lakireddy Bali Reddy college of Engineering",
        "Bapatla Engineering college",
        "SRK Institute of Technology",
        "Andhra Loyola Institute of Engineering and Technology",
        "Velagapudi Ramakrishna Siddhartha Engineering College",
        "PSCMR College of Engineering and Technology",
        "Prasad V. Potluri Siddhartha Institute Of Technology",
        "RK COLLEGE OF ENGINEERING",
        "MIC College of Technology",
        "Amrita Sai Institute of Science and Technology"
    };
    int activeEventCount = 0;
    int totalRegistrationCount = 0;
    for (Event event : events) {
        if (event.active()) {
            activeEventCount++;
        }
        totalRegistrationCount += event.registeredCount();
    }
%>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Organizer Dashboard</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
<header class="topbar organizer-topbar">
    <h1>Organizer Console</h1>
    <nav>
        <span><%=Html.escape(user.fullName())%> · <%=Html.escape(user.rollNo())%></span>
        <form class="inline" action="logout" method="post">
            <button class="btn secondary">Logout</button>
        </form>
    </nav>
</header>

<div class="participant-layout organizer-layout">
    <aside class="sidebar" aria-label="Organizer menu">
        <a class="btn new-event-button" href="event">+ New Event</a>
        <div class="sidebar-title">Organizer Menu</div>
        <nav>
            <a class="side-link <%=Html.escape("dashboard".equals(activeSection) ? "active" : "")%>"
                    href="dashboard">Dashboard</a>
            <a class="side-link <%=Html.escape("events".equals(activeSection) ? "active" : "")%>"
                    href="events">All Events</a>
            <a class="side-link <%=Html.escape("reports".equals(activeSection) ? "active" : "")%>"
                    href="reports">Registration Reports</a>
            <a class="side-link <%=Html.escape("profile".equals(activeSection) ? "active" : "")%>"
                    href="profile">Profile</a>
        </nav>
    </aside>

    <main class="participant-main">
        <section class="dashboard-panel" <%=Html.escape("dashboard".equals(activeSection) ? "" : "hidden")%>>
            <div class="page-heading">
                <h2>Dashboard</h2>
                <p class="muted">Monitor events and participant registrations.</p>
            </div>
            <div class="summary-cards organizer-summary-cards">
                <article class="summary-card">
                    <div class="summary-number"><%=Html.escape(events.size())%></div>
                    <h3>All events</h3>
                    <p>Total events created in the system.</p>
                    <a href="events">Click here</a>
                </article>
                <article class="summary-card active-summary">
                    <div class="summary-number"><%=Html.escape(activeEventCount)%></div>
                    <h3>Active events</h3>
                    <p>Events currently enabled for registration.</p>
                    <a href="events">Click here</a>
                </article>
                <article class="summary-card registered-summary">
                    <div class="summary-number"><%=Html.escape(totalRegistrationCount)%></div>
                    <h3>Registrations</h3>
                    <p>Participants registered across all events.</p>
                    <a href="reports">View reports</a>
                </article>
            </div>
        </section>

        <section class="dashboard-panel" <%=Html.escape("events".equals(activeSection) ? "" : "hidden")%>>
            <div class="toolbar">
                <div>
                    <h2>All Events</h2>
                    <p class="muted">Create, view, edit, and delete campus events.</p>
                </div>
                <a class="btn" href="event">+ New Event</a>
            </div>

            <form class="filter event-filter" action="events" method="get">
                <div>
                    <label for="eventCategory">Category</label>
                    <select id="eventCategory" name="category">
                        <option value="">All categories</option>
                        <% for (String category : new String[]{
                                "Technical", "Cultural", "Sports",
                                "Workshop", "Seminar"}) { %>
                            <option value="<%=Html.escape(category)%>"
                                    <%=Html.escape(category.equals(request.getParameter("category"))
                                            ? "selected" : "")%>>
                                <%=Html.escape(category)%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="eventDepartment">Department</label>
                    <select id="eventDepartment" name="department">
                        <option value="">All departments</option>
                        <% for (String department : new String[]{
                                "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                                "MECH", "CIVIL", "ASE"}) { %>
                            <option value="<%=Html.escape(department)%>"
                                    <%=Html.escape(department.equals(
                                            request.getParameter("department"))
                                                    ? "selected" : "")%>>
                                <%=Html.escape(department)%>
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
                <% for (Event event : events) { %>
                    <article class="card">
                        <img src="<%=Html.escape(event.hasImage()
                                ? "event-image?id=" + event.id()
                                : "https://placehold.co/640x360/12355b/ffffff?text=Campus+Event")%>"
                                alt="<%=Html.escape(event.title())%>">
                        <div class="card-body">
                            <span class="badge <%=Html.escape(event.active()
                                    ? "active-event-badge" : "inactive-event-badge")%>">
                                <%=Html.escape(event.active() ? "Active" : "Inactive")%>
                            </span>
                            <h3><%=Html.escape(event.title())%></h3>
                            <div class="meta">
                                <%=Html.escape(event.department())%> · <%=Html.escape(event.category())%><br>
                                <%=Html.escape(event.date())%> · <%=Html.escape(event.venue())%><br>
                                Registration closes: <%=Html.escape(event.registrationClosesAt())%><br>
                                <strong><%=Html.escape(event.registeredCount())%></strong>
                                / <%=Html.escape(event.capacity())%> registered
                            </div>
                            <div class="actions">
                                <a class="btn" href="event?id=<%=Html.escape(event.id())%>">Edit / View</a>
                                <a class="btn secondary" href="reports?eventId=<%=Html.escape(event.id())%>">
                                    Members
                                </a>
                                <form action="event" method="post"
                                        onsubmit="return confirm('Delete this event and its registrations?')">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="<%=Html.escape(event.id())%>">
                                    <button class="btn danger">Delete</button>
                                </form>
                            </div>
                        </div>
                    </article>
                <% } %>
                <% if (events.isEmpty()) { %>
                    <div class="empty">
                        No events match the selected category and department.
                    </div>
                <% } %>
            </div>
        </section>

        <section class="dashboard-panel" <%=Html.escape("reports".equals(activeSection) ? "" : "hidden")%>>
            <div class="page-heading">
                <h2>Registration Reports</h2>
                <p class="muted">
                    Search and filter participant registrations across all events.
                </p>
            </div>

            <form class="filter" action="reports" method="get">
                <div>
                    <label for="search">Search</label>
                    <input id="search" name="search"
                            value="<%=Html.escape(request.getParameter("search") == null
                                    ? "" : request.getParameter("search"))%>"
                            placeholder="Name, roll, email, phone or pass ID">
                </div>
                <div>
                    <label for="eventId">Event</label>
                    <select id="eventId" name="eventId">
                        <option value="">All events</option>
                        <% for (Event event : events) { %>
                            <option value="<%=Html.escape(event.id())%>"
                                    <%=Html.escape(String.valueOf(event.id()).equals(
                                            request.getParameter("eventId"))
                                                    ? "selected" : "")%>>
                                <%=Html.escape(event.title())%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="collegeName">College</label>
                    <select id="collegeName" name="collegeName">
                        <option value="">All colleges</option>
                        <% for (String college : colleges) { %>
                            <option value="<%=Html.escape(college)%>"
                                    <%=Html.escape(college.equals(request.getParameter("collegeName"))
                                            ? "selected" : "")%>>
                                <%=Html.escape(college)%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="department">Department</label>
                    <select id="department" name="department">
                        <option value="">All departments</option>
                        <% for (String department : new String[]{
                                "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                                "MECH", "CIVIL", "ASE"}) { %>
                            <option value="<%=Html.escape(department)%>"
                                    <%=Html.escape(department.equals(request.getParameter("department"))
                                            ? "selected" : "")%>>
                                <%=Html.escape(department)%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="year">Year</label>
                    <select id="year" name="year">
                        <option value="">All years</option>
                        <% for (int year = 1; year <= 4; year++) { %>
                            <option value="<%=Html.escape(year)%>"
                                    <%=Html.escape(String.valueOf(year).equals(
                                            request.getParameter("year"))
                                                    ? "selected" : "")%>>
                                <%=Html.escape(year)%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div class="report-filter-actions">
                    <button class="btn">Apply filters</button>
                    <a class="btn secondary" href="reports">Clear</a>
                </div>
            </form>

            <div class="report-summary">
                <strong><%=Html.escape(records.size())%></strong> matching registrations
            </div>

            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Event</th>
                            <th>Roll no.</th>
                            <th>Participant</th>
                            <th>Email</th>
                            <th>Phone</th>
                            <th>College</th>
                            <th>Department</th>
                            <th>Year</th>
                            <th>Pass ID</th>
                            <th>Payment</th>
                            <th>Registered</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (RegistrationRecord record : records) { %>
                            <tr>
                                <td><%=Html.escape(record.eventTitle())%></td>
                                <td><%=Html.escape(record.rollNo())%></td>
                                <td><%=Html.escape(record.name())%></td>
                                <td><%=Html.escape(record.email())%></td>
                                <td><%=Html.escape(record.phoneNumber())%></td>
                                <td><%=Html.escape(record.collegeName())%></td>
                                <td><%=Html.escape(record.department())%></td>
                                <td><%=Html.escape(record.year())%></td>
                                <td><%=Html.escape(record.passId())%></td>
                                <td><%=Html.escape(record.paymentStatus())%></td>
                                <td><%=Html.escape(record.registeredAt())%></td>
                            </tr>
                        <% } %>
                        <% if (records.isEmpty()) { %>
                            <tr>
                                <td colspan="11" class="empty-table-cell">
                                    No registrations match the selected filters.
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="dashboard-panel" <%=Html.escape("profile".equals(activeSection) ? "" : "hidden")%>>
            <div class="page-heading">
                <h2>Organizer Profile</h2>
                <p class="muted">Account information for the current organizer.</p>
            </div>
            <article class="organizer-profile-card">
                <div class="organizer-avatar" aria-hidden="true">
                    <%=Html.escape(user.fullName() == null || user.fullName().isBlank()
                            ? "?" : user.fullName().substring(0, 1).toUpperCase())%>
                </div>
                <dl class="details-list">
                    <div><dt>Full name</dt><dd><%=Html.escape(user.fullName())%></dd></div>
                    <div><dt>Organizer ID</dt><dd><%=Html.escape(user.rollNo())%></dd></div>
                    <div><dt>Email</dt><dd><%=Html.escape(user.email())%></dd></div>
                    <div><dt>Role</dt><dd>Organizer</dd></div>
                </dl>
            </article>
        </section>
    </main>
</div>

</body>
</html>
