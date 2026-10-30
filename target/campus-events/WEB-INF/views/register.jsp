<%@ page import="com.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Participant Registration</title>
    <link rel="stylesheet" href="assets/style.css">
    <script defer src="assets/validation.js"></script>
</head>
<body>
<main class="auth wide">
    <h1>Participant Registration</h1>
    <p class="muted">
        Complete all fields to create your participant account.
    </p>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="message error"><%=Html.escape(request.getAttribute("errorMessage"))%></div>
    <% } %>
    <div class="message error client-errors" hidden></div>

    <form id="registrationForm" action="register" method="post"
            enctype="multipart/form-data" data-validate data-strong-password>
        <div class="grid-2">
            <div>
                <label for="fullName">Full name</label>
                <input id="fullName" name="fullName" required maxlength="100">
            </div>
            <div>
                <label for="rollNo">Roll number</label>
                <input id="rollNo" name="rollNo" required maxlength="20">
            </div>

            <div>
                <label for="email">Email</label>
                <input id="email" type="email" name="email" required>
            </div>

            <div>
                <label for="phoneNumber">Phone number</label>
                <input id="phoneNumber" type="tel" name="phoneNumber" required
                        pattern="[6-9][0-9]{9}" maxlength="10"
                        placeholder="10-digit mobile number">
            </div>
            <div>
                <label for="collegeName">College name</label>
                <select id="collegeName" name="collegeName" required>
                    <option value="">Select college</option>
                    <% for (String college : new String[]{
                            "Lakireddy Bali Reddy college of Engineering",
                            "Bapatla Engineering college",
                            "SRK Institute of Technology",
                            "Andhra Loyola Institute of Engineering and Technology",
                            "Velagapudi Ramakrishna Siddhartha Engineering College",
                            "PSCMR College of Engineering and Technology",
                            "Prasad V. Potluri Siddhartha Institute Of Technology",
                            "RK COLLEGE OF ENGINEERING",
                            "MIC College of Technology",
                            "Amrita Sai Institute of Science and Technology"}) { %>
                        <option value="<%=Html.escape(college)%>"><%=Html.escape(college)%></option>
                    <% } %>
                </select>
            </div>

            <div>
                <label for="department">Department</label>
                <select id="department" name="department" required>
                    <option value="">Select</option>
                    <% for (String department : new String[]{
                            "CSE", "IT", "AI&DS", "CSM", "EEE", "ECE",
                            "MECH", "CIVIL", "ASE"}) { %>
                        <option><%=Html.escape(department)%></option>
                    <% } %>
                </select>
            </div>
            <div>
                <label for="year">Year</label>
                <select id="year" name="year" required>
                    <option value="">Select</option>
                    <option value="1">1st Year</option>
                    <option value="2">2nd Year</option>
                    <option value="3">3rd Year</option>
                    <option value="4">4th Year</option>
                </select>
            </div>

            <div>
                <label>Gender</label>
                <div class="options">
                    <label><input type="radio" name="gender" value="Male" required> Male</label>
                    <label><input type="radio" name="gender" value="Female"> Female</label>
                    <label><input type="radio" name="gender" value="Other"> Other</label>
                </div>
            </div>
            <div></div>
        </div>

        <label for="address">Address</label>
        <textarea id="address" name="address" required maxlength="500"></textarea>

        <label for="profilePhoto">Profile photo (JPG/PNG/WEBP, max 2 MB)</label>
        <input id="profilePhoto" type="file" name="profilePhoto"
                accept="image/jpeg,image/png,image/webp" required>

        <div class="grid-2 password-row">
            <div>
                <label for="password">Password</label>
                <input id="password" type="password" name="password"
                        autocomplete="new-password" minlength="6" maxlength="15"
                        pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,15}"
                        title="6–15 characters with lowercase, uppercase, number and special symbol"
                        required>
            </div>
            <div>
                <label for="confirmPassword">Confirm password</label>
                <input id="confirmPassword" type="password" name="confirmPassword"
                        autocomplete="new-password" minlength="6" maxlength="15" required>
            </div>
        </div>

        <p class="field-help password-requirement">
            Password must be a minimum of 6 and a maximum of 15 characters,
            with at least one small letter, capital letter, number, and special symbol.
        </p>

        <div class="options declaration-options">
            <label>
                <input id="showRegistrationPasswords" type="checkbox">
                Show password
            </label>
        </div>
        <div class="actions">
            <button id="submitRegistration" class="btn" type="submit">
                Submit Registration
            </button>
            <button class="btn secondary" type="reset">Reset</button>
            <a class="btn secondary" href="login">Back</a>
        </div>
    </form>
</main>

<script>
(() => {
    const form = document.getElementById('registrationForm');
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const showPasswords = document.getElementById('showRegistrationPasswords');
    showPasswords.addEventListener('change', () => {
        const type = showPasswords.checked ? 'text' : 'password';
        password.type = type;
        confirmPassword.type = type;
    });

    form.addEventListener('reset', () => {
        setTimeout(() => {
            password.type = 'password';
            confirmPassword.type = 'password';
        }, 0);
    });
})();
</script>
</body>
</html>
