# Keycloak Gov.br Level Validator

Este projeto implementa um SPI (Service Provider Interface) para Keycloak que **valida o nível de autenticação Gov.br** durante o fluxo de login, bloqueando usuários com nível insuficiente.

## 📋 Visão Geral

O SPI atua como um **Authenticator** que é executado durante o fluxo de autenticação do Keycloak. Quando um usuário faz login através do provedor Gov.br, o sistema:

1. ✅ Detecta logins realizados via Gov.br
2. 🔍 Extrai o token de acesso da sessão federada
3. 🌐 Consulta a API Gov.br para obter o nível do usuário
4. ⚖️ Valida se o nível atende aos critérios (Prata ou Ouro)
5. ✅ Permite login se aprovado OU ❌ Bloqueia com mensagem de erro

## 🚨 Comportamento de Validação

### ✅ **Login Aprovado**
- **Nível Prata** ou **Nível Ouro** → Login permitido
- Nível salvo como atributo do usuário para referência

### ❌ **Login Bloqueado**
- **Nível Bronze** → Erro: "Nível de autenticação insuficiente"
- **Token inválido** → Erro: "Token de autenticação Gov.br inválido"
- **API indisponível** → Erro: "Serviço temporariamente indisponível"

## 🏗️ Arquitetura do Sistema

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Keycloak      │    │  Gov.br Level    │    │   API Gov.br    │
│   Login Flow    │────│   Validator      │────│   /nivel        │
└─────────────────┘    └──────────────────┘    └─────────────────┘
        │                        │                        │
        │ 1. User Login          │ 2. Extract Token      │ 3. Query Level
        │ via Gov.br             │ 3. Validate Level     │ 4. Return Bronze/
        │                        │ 4. Allow/Block        │    Prata/Ouro
        ▼                        ▼                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ ✅ Success →     │    │ ❌ Error Page     │    │ 📊 Level Data   │
│ Continue Login  │    │ with Message     │    │ {"id":"2",      │
└─────────────────┘    └──────────────────┘    │ "descricao":".."}│
                                               └─────────────────┘
```

## 🛠️ Instalação e Configuração

### 1. Build do Projeto

```bash
# Clone o repositório
git clone <repository-url>
cd keycloak-govbr-level-validator

# Build com Maven
mvn clean package

# JAR será gerado em: target/keycloak-govbr-level-validator-1.0-SNAPSHOT.jar
```

### 2. Deploy no Keycloak

#### **Docker/Podman**
```bash
# Copie o JAR para o container
docker cp target/keycloak-govbr-level-validator-1.0-SNAPSHOT.jar keycloak:/opt/keycloak/providers/

# Reinicie o Keycloak
docker restart keycloak
```

#### **Instalação Standalone**
```bash
# Copie para o diretório providers
cp target/keycloak-govbr-level-validator-1.0-SNAPSHOT.jar $KEYCLOAK_HOME/providers/

# Reinicie o Keycloak
$KEYCLOAK_HOME/bin/kc.sh start
```

### 3. Configuração do Identity Provider Gov.br

```bash
# Configurações no Keycloak Admin Console
Realm → Identity Providers → Add Provider: OpenID Connect

Alias: gov-br
Display Name: Gov.br
Authorization URL: https://sso.staging.acesso.gov.br/auth/realms/govbr/protocol/openid_connect/auth
Token URL: https://sso.staging.acesso.gov.br/auth/realms/govbr/protocol/openid_connect/token
Client ID: [seu-client-id]
Client Secret: [seu-client-secret]
```

### 4. Configuração do Authentication Flow

1. **Acesse**: `Authentication → Flows`
2. **Duplique** o flow "Browser" (ex: "Browser Gov.br")
3. **Adicione Execution** após "Identity Provider Redirector":
   - **Provider**: `Gov.br Level Validator`
   - **Requirement**: `REQUIRED`
4. **Configure** o Realm para usar o novo flow

#### **Posicionamento no Flow**
```
Browser Gov.br Flow:
├── Cookie ✅ ALTERNATIVE
├── Kerberos ✅ DISABLED  
├── Identity Provider Redirector ✅ ALTERNATIVE
├── Gov.br Level Validator ✅ REQUIRED  ← ADICIONAR AQUI
└── Forms ✅ ALTERNATIVE
    ├── Username Password Form ✅ REQUIRED
    └── Browser - Conditional OTP ✅ CONDITIONAL
```

## ⚙️ Configurações

### **URLs da API Gov.br**
```java
// Em GovBrValidatorConfig.java
private static final String NIVEL_API_URL = "https://sso.teste/nivel";
```

### **Níveis Aceitos**
```java
// Configuração atual: apenas Prata e Ouro
private static final String[] ACCEPTED_LEVELS = {"Prata", "Ouro"};
```

### **Timeouts**
```java
private static final int REQUEST_TIMEOUT = 30; // segundos
private static final int CONNECT_TIMEOUT = 10; // segundos
```

## 🔍 Monitoramento e Logs

### **Logs Importantes**
```bash
# Filtrar logs do validator
tail -f $KEYCLOAK_HOME/data/log/keycloak.log | grep "GovBr"

# Logs de sucesso
✅ Validação APROVADA para usuário joao.silva - Nível: Prata

# Logs de bloqueio  
❌ Validação REJEITADA para usuário maria.santos - Nível Bronze insuficiente
```

### **Métricas Monitoradas**
- ✅ Logins aprovados por nível
-