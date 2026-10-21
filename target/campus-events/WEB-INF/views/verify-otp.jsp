<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Organizer Login Verification</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
<main class="auth">
    <h1>Organizer login verification</h1>
    <p class="muted">
        Enter the six-digit security code sent to the organizer email address.
        The code expires in five minutes.
    </p>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="message error"><%=request.getAttribute("errorMessage")%></div>
    <% } %>

    <form action="verify-otp" method="post">
        <label for="otp">Security code</label>
        <input id="otp" name="otp" inputmode="numeric" pattern="[0-9]{6}"
                maxlength="6" autocomplete="one-time-code" required autofocus>

        <div class="actions">
            <button class="btn">Verify and continue</button>
            <a class="btn secondary" href="login">Cancel</a>
        </div>
    </form>
</main>
</body>
</html>
