<%@ page import="com.model.*,java.time.*" contentType="text/html;charset=UTF-8" %>
<%
    Event event = (Event) request.getAttribute("event");
    boolean editing = event != null;
    User user = (User) session.getAttribute("user");
    boolean organizer = "ORGANIZER".equals(user.role());
%>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title><%=editing ? event.title() : "New Event"%></title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
<header class="topbar">
    <h1>Campus Events</h1>
    <nav>
        <a href="dashboard">Dashboard</a>
    </nav>
</header>

<main class="auth wide">
    <h1><%=editing ? "Event details" : "Create event"%></h1>

    <% if (organizer) { %>
        <form id="eventForm" action="event" method="post" enctype="multipart/form-data">
            <input type="hidden" name="id" value="<%=editing ? event.id() : ""%>">

            <div class="grid-2">
                <div>
                    <label for="title">Event title</label>
                    <input id="title" name="title" required
                            value="<%=editing ? event.title() : ""%>">
                </div>
                <div>
                    <label for="category">Category</label>
                    <select id="category" name="category" required>
                        <% for (String category : new String[]{
                                "Technical", "Cultural", "Sports", "Workshop", "Seminar"}) { %>
                            <option <%=editing && category.equals(event.category())
                                    ? "selected" : ""%>><%=category%></option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="department">Department</label>
                    <select id="department" name="department" required>
                        <option value="">Select department</option>
                        <% for (String department : new String[]{
                                "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                                "MECH", "CIVIL", "ASE"}) { %>
                            <option value="<%=department%>"
                                    <%=editing && department.equals(event.department())
                                            ? "selected" : ""%>>
                                <%=department%>
                            </option>
                        <% } %>
                    </select>
                </div>
                <div>
                    <label for="venue">Venue</label>
                    <input id="venue" name="venue" required
                            value="<%=editing ? event.venue() : ""%>">
                </div>
                <div>
                    <label for="eventDate">Event date</label>
                    <input id="eventDate" type="date" name="eventDate" required
                            <%=editing ? "" : "min=\"" + LocalDate.now() + "\""%>
                            value="<%=editing ? event.date().toLocalDate() : ""%>">
                </div>
                <div>
                    <label for="eventTime">Event start time</label>
                    <input id="eventTime" type="time" name="eventTime" required
                            value="<%=editing ? event.date().toLocalTime() : ""%>">
                </div>
                <div>
                    <label for="registrationCloseDate">Registration closing date</label>
                    <input id="registrationCloseDate" type="date"
                            name="registrationCloseDate" required
                            <%=editing ? "" : "disabled"%>
                            <%=editing ? "" : "min=\"" + LocalDate.now() + "\""%>
                            value="<%=editing
                                    ? event.registrationClosesAt().toLocalDate() : ""%>">
                </div>
                <div>
                    <label for="registrationCloseTime">Registration closing time</label>
                    <input id="registrationCloseTime" type="time"
                            name="registrationCloseTime" required
                            value="<%=editing
                                    ? event.registrationClosesAt().toLocalTime() : ""%>">
                </div>
                <div>
                    <label for="capacity">Capacity</label>
                    <input id="capacity" type="number" min="1" name="capacity" required
                            value="<%=editing ? event.capacity() : 100%>">
                </div>
                <div>
                    <label for="fee">Fee (₹)</label>
                    <input id="fee" type="number" min="0" step="0.01" name="fee" required
                            value="<%=editing ? event.fee() : 0%>">
                </div>
            </div>

            <label for="description">Description</label>
            <textarea id="description" name="description" required><%=editing
                    ? event.description() : ""%></textarea>

            <label for="eventImage">Event image</label>
            <input id="eventImage" type="file" name="eventImage"
                    accept="image/jpeg,image/png,image/webp"
                    <%=editing ? "" : "required"%>>
            <p class="field-help">
                Upload a JPG, PNG, or WEBP image up to 5 MB.
                <%=editing && event.hasImage()
                        ? "Leave this empty to keep the current image." : ""%>
            </p>

            <% if (editing && event.hasImage()) { %>
                <img class="event-image-preview" src="event-image?id=<%=event.id()%>"
                        alt="Current image for <%=event.title()%>">
            <% } %>

            <div class="options declaration-options">
                <label>
                    <input type="checkbox" name="active"
                            <%=!editing || event.active() ? "checked" : ""%>>
                    Available for registration
                </label>
            </div>

            <div class="actions">
                <button class="btn">Save event</button>
                <button type="reset" class="btn secondary">Reset</button>
                <a class="btn secondary" href="events">Back</a>
            </div>
        </form>
    <% } else { %>
        <% if (event.hasImage()) { %>
            <img class="event-detail-image" src="event-image?id=<%=event.id()%>"
                    alt="<%=event.title()%>">
        <% } %>
        <p><%=event.description()%></p>
        <dl class="details-list compact-details">
            <div><dt>Date</dt><dd><%=event.date()%></dd></div>
            <div><dt>Department</dt><dd><%=event.department()%></dd></div>
            <div>
                <dt>Registration closes</dt>
                <dd><%=event.registrationClosesAt()%></dd>
            </div>
            <div><dt>Venue</dt><dd><%=event.venue()%></dd></div>
            <div><dt>Fee</dt><dd>₹<%=String.format("%.2f", event.fee())%></dd></div>
        </dl>
        <div class="actions">
            <a class="btn secondary" href="<%=request.getAttribute("backPath")%>">
                Back
            </a>
        </div>
    <% } %>
</main>

<script>
(() => {
    const form = document.getElementById('eventForm');
    if (!form) {
        return;
    }

    const eventDateField = document.getElementById('eventDate');
    const registrationCloseDateField =
            document.getElementById('registrationCloseDate');

    function updateRegistrationDateLimit() {
        const eventDate = eventDateField.value;
        registrationCloseDateField.disabled = !eventDate;
        registrationCloseDateField.max = eventDate;

        if (registrationCloseDateField.value > eventDate) {
            registrationCloseDateField.value = '';
        }
    }

    eventDateField.addEventListener('change', updateRegistrationDateLimit);
    form.addEventListener('reset', () => {
        setTimeout(updateRegistrationDateLimit, 0);
    });
    updateRegistrationDateLimit();

    form.addEventListener('submit', event => {
        const eventStart = new Date(
                document.getElementById('eventDate').value
                + 'T'
                + document.getElementById('eventTime').value);
        const registrationClose = new Date(
                document.getElementById('registrationCloseDate').value
                + 'T'
                + document.getElementById('registrationCloseTime').value);

        if (registrationClose >= eventStart) {
            event.preventDefault();
            window.alert('Registration closing date and time must be before the event starts.');
        }
    });
})();
</script>
</body>
</html>
