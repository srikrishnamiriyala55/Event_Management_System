<%@ page import="com.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Login | Campus Events</title>
    <link rel="stylesheet" href="assets/style.css">
    <script defer src="assets/validation.js"></script>
</head>
<body class="login-page">
<main class="auth login-card">
    <header class="login-card-header">
        <span class="login-logo" aria-hidden="true">CE</span>
        <div>
            <h1>Campus Events</h1>
            <p>Campus Event Management System</p>
        </div>
    </header>

    <div class="login-heading">
        <h2>Sign in</h2>
        <p class="muted">Use your roll number or organizer ID to continue.</p>
    </div>

    <% if (request.getAttribute("statusMessage") != null) { %>
        <div class="message"><%=Html.escape(request.getAttribute("statusMessage"))%></div>
    <% } %>
    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="message error"><%=Html.escape(request.getAttribute("errorMessage"))%></div>
    <% } %>

    <div class="message error client-errors" hidden></div>
    <form class="login-form" action="login" method="post" data-validate>
        <label for="loginRollNo">Roll number / Organizer ID</label>
        <input id="loginRollNo" name="rollNo" maxlength="20" required
                autocomplete="username" placeholder="Enter your ID" autofocus>

        <label for="loginPassword">Password</label>
        <div class="password-field">
            <input id="loginPassword" type="password" name="password" required
                    autocomplete="current-password" placeholder="Enter your password">
            <button id="toggleLoginPassword" class="password-toggle" type="button"
                    aria-label="Show password" title="Show password">&#128065;</button>
        </div>

        <div class="actions login-actions">
            <button class="btn login-submit" type="submit">Sign in</button>
            <button class="btn secondary" type="reset">Reset</button>
        </div>
    </form>

    <div class="login-register">
        <span>New participant?</span>
        <a href="register">Create an account</a>
    </div>
</main>
<script>
(() => {
    const password = document.getElementById('loginPassword');
    const toggle = document.getElementById('toggleLoginPassword');

    toggle.addEventListener('click', () => {
        const showing = password.type === 'text';
        password.type = showing ? 'password' : 'text';
        toggle.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
        toggle.setAttribute('title', showing ? 'Show password' : 'Hide password');
    });
})();
</script>
</body>
</html>
