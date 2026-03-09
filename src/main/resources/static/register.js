
const passwordInput = document.getElementById('password');
const confirmPasswordInput = document.getElementById('confirmPassword');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailInput = document.getElementById('email');
const messageEl = document.getElementById('message');
const submitBtn = document.getElementById('submitBtn');
const registerForm = document.getElementById('registerForm');


function checkPasswordsMatch() {
    const pwd = passwordInput.value;
    const confirmPwd = confirmPasswordInput.value;

    if (confirmPwd.length > 0) {
        if (pwd !== confirmPwd) {
            confirmPasswordInput.style.borderColor = '#ef4444';
            confirmPasswordInput.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
        } else {
            confirmPasswordInput.style.borderColor = '#22c55e';
            confirmPasswordInput.style.backgroundColor = 'rgba(34, 197, 94, 0.1)';
        }
    } else {
        confirmPasswordInput.style.borderColor = '#334155';
        confirmPasswordInput.style.backgroundColor = '#0f172a';
    }
}

passwordInput.addEventListener('input', checkPasswordsMatch);
confirmPasswordInput.addEventListener('input', checkPasswordsMatch);

sendCodeBtn.addEventListener('click', async () => {
    const email = emailInput.value.trim();

    if (!email) {
        messageEl.textContent = "Por favor, digite seu e-mail primeiro.";
        messageEl.className = "message error";
        emailInput.focus();
        return;
    }

    sendCodeBtn.disabled = true;
    sendCodeBtn.textContent = "A enviar...";
    messageEl.textContent = "";

    try {
        const response = await fetch('/api/auth/send-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        });

        if (response.ok) {
            messageEl.textContent = "Código enviado para o seu e-mail!";
            messageEl.className = "message success";

            let timeLeft = 60;
            sendCodeBtn.textContent = `${timeLeft}s`;

            const timer = setInterval(() => {
                timeLeft--;
                sendCodeBtn.textContent = `${timeLeft}s`;

                if (timeLeft <= 0) {
                    clearInterval(timer);
                    sendCodeBtn.disabled = false;
                    sendCodeBtn.textContent = "Enviar Código";
                }
            }, 1000);

        } else {
            const errorText = await response.text();
            messageEl.textContent = errorText;
            messageEl.className = "message error";
            sendCodeBtn.disabled = false;
            sendCodeBtn.textContent = "Enviar Código";
        }
    } catch (error) {
        messageEl.textContent = "Erro ao conectar com o servidor.";
        messageEl.className = "message error";
        sendCodeBtn.disabled = false;
        sendCodeBtn.textContent = "Enviar Código";
    }
});

registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value.trim();
    const email = emailInput.value.trim();
    const emailCode = document.getElementById('emailCode').value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    if (password !== confirmPassword) {
        messageEl.textContent = "As senhas não coincidem. Corrija para continuar.";
        messageEl.className = "message error";
        return;
    }

    submitBtn.textContent = 'A criar conta...';
    submitBtn.disabled = true;
    messageEl.textContent = '';
    messageEl.className = 'message';

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, emailCode, password })
        });

        if (response.ok) {
            messageEl.textContent = "Conta criada com sucesso! A redirecionar...";
            messageEl.className = "message success";

            submitBtn.style.backgroundColor = '#22c55e';
            submitBtn.textContent = 'Sucesso!';

            setTimeout(() => {
                window.location.href = 'index.html';
            }, 2000);
        } else {
            const errorText = await response.text();
            messageEl.textContent = errorText;
            messageEl.className = "message error";

            submitBtn.textContent = 'Criar Conta';
            submitBtn.disabled = false;
        }
    } catch (error) {
        messageEl.textContent = "Erro crítico de conexão com o servidor.";
        messageEl.className = "message error";

        submitBtn.textContent = 'Criar Conta';
        submitBtn.disabled = false;
    }
});