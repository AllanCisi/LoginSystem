
const step1 = document.getElementById('step-1');
const step2 = document.getElementById('step-2');
const step3 = document.getElementById('step-3');

const emailInput = document.getElementById('email');
const resetCodeInput = document.getElementById('resetCode');
const totpCodeInput = document.getElementById('totpCode');
const newPasswordInput = document.getElementById('newPassword');
const confirmPasswordInput = document.getElementById('confirmPassword');

const msgStep1 = document.getElementById('msgStep1');
const msgStep2 = document.getElementById('msgStep2');
const msgStep3 = document.getElementById('msgStep3');

const sendCodeBtn = document.getElementById('sendCodeBtn');
const btnNextStep1 = document.getElementById('btnNextStep1');
const btnNextStep2 = document.getElementById('btnNextStep2');
const btnComplete = document.getElementById('btnComplete');

let has2fa = false;

function switchStep(hideEl, showEl) {
    hideEl.classList.add('hidden');
    showEl.classList.remove('hidden');
}

function checkPasswordsMatch() {
    const pwd = newPasswordInput.value;
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
newPasswordInput.addEventListener('input', checkPasswordsMatch);
confirmPasswordInput.addEventListener('input', checkPasswordsMatch);

sendCodeBtn.addEventListener('click', async () => {
    const email = emailInput.value.trim();
    if (!email) {
        msgStep1.textContent = "Digite o e-mail primeiro.";
        msgStep1.className = "message error";
        return;
    }

    sendCodeBtn.disabled = true;
    sendCodeBtn.textContent = "Enviando...";
    msgStep1.textContent = "";

    try {
        const response = await fetch('/api/auth/send-reset-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        });

        if (response.ok) {
            msgStep1.textContent = "Código enviado para o e-mail!";
            msgStep1.className = "message success";

            let timeLeft = 60;
            sendCodeBtn.textContent = `${timeLeft}s`;
            const timer = setInterval(() => {
                timeLeft--;
                sendCodeBtn.textContent = `${timeLeft}s`;
                if (timeLeft <= 0) {
                    clearInterval(timer);
                    sendCodeBtn.disabled = false;
                    sendCodeBtn.textContent = "Reenviar";
                }
            }, 1000);
        } else {
            msgStep1.textContent = "Erro ao enviar código.";
            msgStep1.className = "message error";
            sendCodeBtn.disabled = false;
            sendCodeBtn.textContent = "Enviar Código";
        }
    } catch (error) {
        msgStep1.textContent = "Erro de conexão.";
        msgStep1.className = "message error";
        sendCodeBtn.disabled = false;
        sendCodeBtn.textContent = "Enviar Código";
    }
});

document.getElementById('step1Form').addEventListener('submit', async (e) => {
    e.preventDefault();
    btnNextStep1.disabled = true;
    btnNextStep1.textContent = "A verificar...";

    try {
        const response = await fetch('/api/auth/verify-reset-step1', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: emailInput.value, code: resetCodeInput.value })
        });

        if (response.ok) {
            const data = await response.json();
            has2fa = data.require2fa;

            msgStep1.textContent = "";
            if (has2fa) {
                switchStep(step1, step2);
                totpCodeInput.focus();
            } else {
                switchStep(step1, step3);
                newPasswordInput.focus();
            }
        } else {
            const err = await response.text();
            msgStep1.textContent = err;
            msgStep1.className = "message error";
        }
    } catch (e) {
        msgStep1.textContent = "Erro de conexão.";
        msgStep1.className = "message error";
    }
    btnNextStep1.disabled = false;
    btnNextStep1.textContent = "Avançar →";
});

document.getElementById('step2Form').addEventListener('submit', (e) => {
    e.preventDefault();
    if(totpCodeInput.value.trim().length < 6) {
        msgStep2.textContent = "Digite o código de 6 dígitos.";
        msgStep2.className = "message error";
        return;
    }
    msgStep2.textContent = "";
    switchStep(step2, step3);
    newPasswordInput.focus();
});

document.getElementById('step3Form').addEventListener('submit', async (e) => {
    e.preventDefault();

    if (newPasswordInput.value !== confirmPasswordInput.value) {
        msgStep3.textContent = "As senhas não coincidem!";
        msgStep3.className = "message error";
        return;
    }

    btnComplete.textContent = "A finalizar...";
    btnComplete.disabled = true;

    try {
        const payload = {
            email: emailInput.value.trim(),
            code: resetCodeInput.value.trim(),
            newPassword: newPasswordInput.value,
            totpCode: has2fa ? totpCodeInput.value.trim() : null
        };

        const response = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            msgStep3.textContent = "Senha alterada com sucesso! Redirecionando...";
            msgStep3.className = "message success";
            btnComplete.style.backgroundColor = '#22c55e';
            btnComplete.textContent = 'Concluído!';
            setTimeout(() => { window.location.href = 'index.html'; }, 2000);
        } else {
            const err = await response.text();
            // Se o erro for do autenticador, voltamos para o passo 2
            if (err.includes("Autenticador")) {
                switchStep(step3, step2);
                msgStep2.textContent = err;
                msgStep2.className = "message error";
                totpCodeInput.value = '';
                totpCodeInput.focus();
            } else {
                msgStep3.textContent = err;
                msgStep3.className = "message error";
            }
            btnComplete.textContent = "Concluir Alteração";
            btnComplete.disabled = false;
        }
    } catch (error) {
        msgStep3.textContent = "Erro de conexão.";
        msgStep3.className = "message error";
        btnComplete.textContent = "Concluir Alteração";
        btnComplete.disabled = false;
    }
});

document.getElementById('btnBackTo1').addEventListener('click', () => switchStep(step2, step1));
document.getElementById('btnBackToPrevious').addEventListener('click', () => {
    if (has2fa) switchStep(step3, step2);
    else switchStep(step3, step1);
});