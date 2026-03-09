// Elementos UI
const emailDisplay = document.getElementById('emailDisplay');
const usernameDisplay = document.getElementById('usernameDisplay');
const mfaBadge = document.getElementById('mfaBadge');
const setup2faSection = document.getElementById('setup2faSection');
const qrCodeImg = document.getElementById('qrCodeImg');
const verify2faForm = document.getElementById('verify2faForm');
const code2faInput = document.getElementById('code2fa');
const verifyBtn = document.getElementById('verifyBtn');
const messageEl = document.getElementById('message');
const logoutBtn = document.getElementById('logoutBtn');

let tempSecret = '';
let csrfHeaderName = 'X-XSRF-TOKEN';
let csrfTokenValue = '';

async function loadProfile() {
    try {
        const response = await fetch('/api/profile/me');

        if (response.status === 401 || response.status === 403) {
            window.location.href = 'index.html'; // Não está logado
            return;
        }

        const data = await response.json();

        emailDisplay.textContent = data.email;
        usernameDisplay.textContent = "@" + data.username;

        if (data.csrfHeader && data.csrfToken) {
            csrfHeaderName = data.csrfHeader;
            csrfTokenValue = data.csrfToken;
        }

        if (data.mfaEnabled) {
            mfaBadge.textContent = '2FA ATIVADO';
            mfaBadge.classList.remove('disabled');
            setup2faSection.classList.add('hidden');
        } else {
            mfaBadge.textContent = '2FA DESATIVADO';
            mfaBadge.classList.add('disabled');
            setup2faSection.classList.remove('hidden');

            qrCodeImg.src = data.qrCode;
            tempSecret = data.tempSecret;
        }
    } catch (error) {
        console.error("Erro ao carregar perfil:", error);
    }
}

verify2faForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    verifyBtn.disabled = true;
    verifyBtn.textContent = "A verificar...";

    const headers = { 'Content-Type': 'application/json' };
    if (csrfTokenValue) {
        headers[csrfHeaderName] = csrfTokenValue; // Ex: 'X-XSRF-TOKEN': 'ab12c3...'
    }

    try {
        const response = await fetch('/api/profile/2fa/enable', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ code: code2faInput.value.trim(), secret: tempSecret })
        });

        if (response.ok) {
            messageEl.textContent = "2FA Ativado com sucesso!";
            messageEl.className = "message success";
            verifyBtn.style.backgroundColor = '#22c55e';
            verifyBtn.textContent = "Protegido!";

            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            const errorText = await response.text();
            messageEl.textContent = errorText;
            messageEl.className = "message error";
            verifyBtn.disabled = false;
            verifyBtn.textContent = "Verificar e Ativar";
        }
    } catch (error) {
        messageEl.textContent = "Erro de conexão.";
        messageEl.className = "message error";
        verifyBtn.disabled = false;
        verifyBtn.textContent = "Verificar e Ativar";
    }
});

logoutBtn.addEventListener('click', async () => {
    const headers = {};
    if (csrfTokenValue) {
        headers[csrfHeaderName] = csrfTokenValue;
    }

    await fetch('/api/auth/logout', {
        method: 'POST',
        headers: headers
    });
    window.location.href = 'index.html';
});

loadProfile();