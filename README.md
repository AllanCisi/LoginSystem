# Sistema de Login com Autenticação e Criptografia (LoginSystem)

## 1. Visão Geral

Este projeto é um sistema de autenticação e segurança construído com Java e Spring Boot, utilizando Spring Security 6. Ele implementa práticas modernas de defesa e controle de acessos.

O sistema gerencia autenticação local e social (Google), proteção contra CSRF avançada (Synchronizer Token via Payload), envio de e-mails com expiração de códigos em memória (Redis) e dupla camada de criptografia (Argon2 + Pepper). Além disso, conta com um fluxo robusto de Autenticação de 2 Fatores (2FA/TOTP) integrado com o Google Authenticator.

### Tecnologias utilizadas

Linguagem & Frameworks: Java, Spring Boot, Spring Security 6, Spring Data JPA, Hibernate.

Banco de Dados: PostgreSQL (Persistência principal) e Redis (Armazenamento em memória para OTPs).

Autenticação e Segurança: OAuth2 (Google Login), TOTP (2FA), Argon2 (Hashing), Chave Pepper (Variável de ambiente), CSRF Token em Payload.

Comunicação: JavaMailSender (Serviço SMTP).

Frontend: HTML5, CSS3, JavaScript Vanilla (Fetch API assíncrona).

## 2. Estrutura do Projeto

### Pacotes principais (me.cisi.LoginSystem)

config: Arquivos de configuração global, incluindo o SecurityConfig (provedor customizado, OAuth2 e CORS) e o CsrfCookieFilter.

controller: * AuthController – Endpoints públicos de login, registro, recuperação de senha e 2FA.

ProfileController – Endpoints privados para gerenciamento de perfil, requisição e validação de QR Code para 2FA.

entity: User – Representação da entidade de usuário com propriedades de MFA.

dto: AuthRequest, RegisterRequest – Objetos de transferência de dados imutáveis.

repository: UserRepository – Persistência e consultas ao PostgreSQL.

service: * PasswordEncryptionService – Lida com Argon2 e a chave Pepper.

EmailVerificationService – Envio de OTPs com controle de expiração via Redis.

MfaService – Geração e validação de chaves TOTP (Google Authenticator).

CustomUserDetailsService – Integração da entidade User com o contexto do Spring Security.

## 3. Funcionalidades

### 3.1 Autenticação e Registro

Login Local: Autenticação padrão via E-mail e Senha.

Login Social (OAuth2): Integração com o Google. Usuários não cadastrados são provisionados automaticamente de forma blindada (UUID), impossibilitando acesso via senha padrão.

Registro: Criação de nova conta precedida de validação de e-mail (Código de 6 dígitos) com tempo de expiração via Redis.

### 3.2 Segurança e Criptografia (Destaque)

Argon2 + Pepper: As senhas não utilizam apenas um "Salt" comum. O projeto utiliza o algoritmo Argon2 (resistente a ataques de força bruta com GPU) somado a uma chave secreta Pepper armazenada apenas nas variáveis de ambiente, inutilizando os hashes em caso de vazamento do banco de dados.

Defesa CSRF: Implementação de Synchronizer Token Pattern enviando o token dinamicamente no Payload das requisições JSON para contornar problemas de cookies bloqueados pelo navegador.

### 3.3 Autenticação de 2 Fatores (2FA/TOTP)

O usuário pode habilitar o MFA gerando um QR Code.

Integração no Login: Se a conta possuir 2FA, o login só é completado após a inserção do código do Autenticador.

Integração na Recuperação de Senha (Wizard): O fluxo de "Esqueci a Senha" funciona em etapas. Caso a conta possua 2FA, o sistema exige obrigatoriamente o código do Autenticador no Passo 2 antes de permitir a alteração da senha.

## 4. Endpoints Principais

Método

Endpoint

Descrição

POST

/api/auth/register

Cria usuário validando o OTP enviado por e-mail

POST

/api/auth/login

Autentica usuário (ou exige 2FA se ativado)

POST

/api/auth/login/verify-2fa

Conclui o login recebendo o código TOTP

POST

/api/auth/send-code

Dispara e-mail de registro (Armazena no Redis por 10 min)

POST

/api/auth/send-reset-code

Dispara e-mail de recuperação de senha

POST

/api/auth/verify-reset-step1

Verifica o OTP de recuperação e avisa se a conta exige 2FA

POST

/api/auth/reset-password

Valida 2FA (se ativado), OTP e salva a nova senha

POST

/api/auth/logout

Invalida a sessão e o SecurityContext

GET

/api/profile/me

Retorna dados do usuário, status do 2FA, e gera o CSRF Token

POST

/api/profile/2fa/enable

Valida o QR Code do usuário e ativa a proteção na conta

## 5. Pré-requisitos

Java 17 ou superior

Maven

PostgreSQL

Redis

Conta no Google Cloud (Para OAuth2 Client ID e Secret)

E-mail para disparo (App Password do Gmail)

## 6. Configurações do projeto

### 6.1 .env

Crie um arquivo .env na raiz do seu projeto baseando-se nas variáveis abaixo:

### Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/login_system
DB_USER=postgres
DB_PASS=sua_senha_do_postgres

### Redis
REDIS_HOST=localhost
REDIS_PORT=6379

### Criptografia de Segurança
SECURITY_PEPPER=sua_chave_secreta_super_longa_e_aleatoria

### Configurações de E-mail (SMTP)
EMAIL_USER=seu.email@gmail.com
EMAIL_PASS=sua_senha_de_app_do_google

### Configurações OAuth2 (Google Cloud Console)
GOOGLE_CLIENT_ID=seu_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=seu_client_secret


## 7. Notas de Arquitetura

Sessão: A autenticação é mantida via HttpSessionSecurityContextRepository, alinhada ao padrão arquitetural de sessões seguras.

Controle de Fluxo UI: O Frontend utiliza manipulação de DOM Vanilla para criar a experiência de "Single Page Application" (SPA) durante os fluxos complexos como o Wizard de recuperação de senha.

Proteção Google OAuth: Contas originadas via botão Social recebem uma "senha" UUID randômica. Uma trava de segurança no Controller impede que esses usuários tentem usar a função "Esqueci a Senha" para burlar a obrigatoriedade do login pelo provedor de identidade do Google.

# Autor
Desenvolvido por Allan Cisi
https://github.com/AllanCisi
