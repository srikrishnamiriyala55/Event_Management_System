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
        Verify your email first. The Submit Registration button is enabled only
        after successful OTP verification.
    </p>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="message error"><%=request.getAttribute("errorMessage")%></div>
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
                <div class="actions compact-actions">
                    <button id="sendOtp" class="btn secondary" type="button">Send OTP</button>
                </div>
            </div>
            <div>
                <label for="emailOtp">Email OTP</label>
                <input id="emailOtp" inputmode="numeric" pattern="[0-9]{6}"
                        maxlength="6" disabled>
                <div class="actions compact-actions">
                    <button id="verifyOtp" class="btn secondary" type="button" disabled>
                        Verify OTP
                    </button>
                </div>
                <div id="otpMessage" class="message otp-message" role="status"
                        aria-live="polite" hidden></div>
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
                        <option value="<%=college%>"><%=college%></option>
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
                        <option><%=department%></option>
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
            <button id="submitRegistration" class="btn" type="submit" disabled>
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
    const email = document.getElementById('email');
    const otp = document.getElementById('emailOtp');
    const send = document.getElementById('sendOtp');
    const verify = document.getElementById('verifyOtp');
    const submit = document.getElementById('submitRegistration');
    const message = document.getElementById('otpMessage');
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const showPasswords = document.getElementById('showRegistrationPasswords');
    let verifiedEmail = '';

    async function callOtpService(action) {
        const body = new URLSearchParams({
            action,
            email: email.value.trim(),
            otp: otp.value.trim()
        });
        const response = await fetch('registration-email-otp', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body
        });
        return response.json();
    }

    function showMessage(text, isSuccess) {
        message.hidden = false;
        message.className = 'message otp-message' + (isSuccess ? ' success' : ' error');
        message.textContent = text;
    }

    send.addEventListener('click', async () => {
        submit.disabled = true;
        verifiedEmail = '';
        showMessage('Sending OTP...', true);

        try {
            const result = await callOtpService('send');
            showMessage(result.message, result.success);
            if (result.success) {
                otp.disabled = false;
                verify.disabled = false;
                email.readOnly = true;
                otp.focus();
            }
        } catch (error) {
            showMessage('Unable to send OTP. Please try again.', false);
        }
    });

    verify.addEventListener('click', async () => {
        if (!/^[0-9]{6}$/.test(otp.value.trim())) {
            showMessage('Enter the six-digit OTP sent to your email.', false);
            return;
        }

        showMessage('Verifying OTP...', true);
        try {
            const result = await callOtpService('verify');
            showMessage(result.message, result.success);
            if (result.success) {
                verifiedEmail = email.value.trim().toLowerCase();
                submit.disabled = false;
                otp.disabled = true;
                verify.disabled = true;
            }
        } catch (error) {
            showMessage('Unable to verify OTP. Please try again.', false);
        }
    });

    showPasswords.addEventListener('change', () => {
        const type = showPasswords.checked ? 'text' : 'password';
        password.type = type;
        confirmPassword.type = type;
    });

    email.addEventListener('input', () => {
        if (email.value.trim().toLowerCase() !== verifiedEmail) {
            submit.disabled = true;
        }
    });

    form.addEventListener('reset', () => {
        setTimeout(() => {
            email.readOnly = false;
            otp.disabled = true;
            verify.disabled = true;
            submit.disabled = true;
            password.type = 'password';
            confirmPassword.type = 'password';
            verifiedEmail = '';
            message.hidden = true;
        }, 0);
    });
})();
</script>
</body>
</html>
