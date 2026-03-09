document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const messageEl = document.getElementById('message');
    const submitBtn = document.getElementById('submitBtn');

    submitBtn.textContent = 'Autenticando...';
    submitBtn.disabled = true;
    messageEl.textContent = '';
    messageEl.className = 'message';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password }),
            credentials: 'include'
        });

        if (response.ok) {
            messageEl.textContent = 'Login aprovado! Entrando...';
            messageEl.classList.add('success');

            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1000);
        } else {
            const errorText = await response.text();
            messageEl.textContent = errorText;
            messageEl.classList.add('error');
            submitBtn.textContent = 'Login';
            submitBtn.disabled = false;
        }
    } catch (error) {
        messageEl.textContent = 'Erro ao conectar com o servidor.';
        messageEl.classList.add('error');
        submitBtn.textContent = 'Entrar de forma segura';
        submitBtn.disabled = false;
    }
});