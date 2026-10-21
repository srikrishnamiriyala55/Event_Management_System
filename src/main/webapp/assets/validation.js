document.addEventListener("DOMContentLoaded", () => {
    const validatedForms = document.querySelectorAll("form[data-validate]");

    validatedForms.forEach((form) => {
        form.addEventListener("submit", (event) => {
            const errors = [];

            form.querySelectorAll("[required]").forEach((field) => {
                if (!field.value.trim()) {
                    errors.push(`${field.dataset.label || field.name} is required`);
                }
            });

            const rollNumber = form.querySelector("[name=rollNo]");
            if (rollNumber && !/^[A-Za-z0-9]{5,20}$/.test(rollNumber.value)) {
                errors.push("Roll number must contain 5–20 letters or numbers");
            }

            const password = form.querySelector("[name=password]");
            const requiresStrongPassword = form.dataset.strongPassword !== undefined;
            const strongPasswordPattern =
                    /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,15}$/;

            if (password && requiresStrongPassword
                    && !strongPasswordPattern.test(password.value)) {
                errors.push(
                    "Password must be 6–15 characters and include lowercase, "
                    + "uppercase, number and special symbol"
                );
            }

            const confirmation = form.querySelector("[name=confirmPassword]");
            if (confirmation && password && confirmation.value !== password.value) {
                errors.push("Passwords do not match");
            }

            const formContainer = form.closest(".login-form-wrap, .auth") || document;
            const errorBox = form.querySelector(".client-errors")
                    || formContainer.querySelector(".client-errors");
            if (errors.length > 0) {
                event.preventDefault();
                if (errorBox) {
                    errorBox.textContent = errors.join(". ");
                    errorBox.hidden = false;
                }
            } else if (errorBox) {
                errorBox.hidden = true;
            }
        });
    });
});
