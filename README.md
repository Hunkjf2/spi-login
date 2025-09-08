# Keycloak SPI - Selo e Nível Gov.br

Este projeto implementa um SPI (Service Provider Interface) para Keycloak que integra com os serviços do Gov.br para obter e armazenar informações de **selo de confiabilidade** e **nível de autenticação** dos usuários.

## 📋 Visão Geral

O SPI atua como um Event Listener que é acionado automaticamente quando um usuário realiza login através do provedor de identidade Gov.br. Durante o processo de autenticação, o sistema:

1. Detecta logins realizados via Gov.br
2. Extrai o token de acesso da sessão federada
3. Consulta as APIs do Gov.br para obter informações de selo e nível
4. Armazena essas informações como atributos do usuário no Keycloak

## 🚀 Funcionalidades

- **Detecção automática** de logins via Gov.br
- **Consulta às APIs** de selo e nível do Gov.br
- **Mapeamento de selos** para descrições legíveis
- **Conversão de níveis** numéricos para descrições (Bronze, Prata, Ouro)
- **Armazenamento seguro** dos dados como atributos do usuário
- **Logs detalhados** para monitoramento e debugging

## 📦 Estrutura do Projeto

```
src/main/java/br/com/spi/govbr/
├── config/
│   └── GovBrConfig.java           # Configurações centralizadas
├── constants/
│   └── GovBrConstants.java        # Constantes dos níveis
├── dto/
│   ├── GovBrAccount.java          # DTO para dados da conta
│   ├── NivelApiResponse.java      # DTO para resposta da API de nível
│   └── SeloInfo.java              # DTO para informações do selo
├── listener/
│   ├── GovBrEventListener.java    # Event Listener principal
│   └── GovBrEventListenerFactory.java # Factory do Event Listener
├── mapper/
│   ├── NivelMapping.java          # Mapeamento de níveis
│   └── SeloMapping.java           # Mapeamento de selos
├── service/
│   ├── ClientService.java         # Cliente HTTP
│   └── GovBrApiService.java       # Serviço principal da API
└── util/
    ├── NivelGovBrConverter.java   # Conversor de nível
    ├── SeloGovBrConverter.java    # Conversor de selo
    └── TokenGovBrConverter.java   # Extrator de token
```

## ⚙️ Configuração - Classe GovBrConfig

A classe `GovBrConfig` centraliza todas as configurações necessárias para a integração com o Gov.br:

### Variáveis de Configuração

| Variável | Valor | Descrição |
|----------|-------|-----------|
| **NIVEL_URL** | `https://sso.teste/nivel` | URL da API do Gov.br para consulta do nível de autenticação do usuário |
| **SELO_URL** | `https://sso.test/confiabilidade` | URL da API do Gov.br para consulta dos selos de confiabilidade |
| **SELO** | `selo` | Nome do atributo que será criado no usuário do Keycloak para armazenar as informações dos selos |
| **NIVEL** | `nivel` | Nome do atributo que será criado no usuário do Keycloak para armazenar o nível de autenticação |
| **PROVIDER_ALIAS_GOVBR** | `gov-br` | Alias/identificador do provedor de identidade Gov.br configurado no Keycloak |
| **PROVIDER_ID_GOVBR** | `govbr-event-listener` | ID único deste Event Listener para registro no Keycloak |

### Métodos de Acesso

- `getNivelUrl()` - Retorna a URL da API de nível
- `getSeloUrl()` - Retorna a URL da API de selo
- `getNivel()` - Retorna o nome do atributo de nível
- `getSelo()` - Retorna o nome do atributo de selo
- `getProviderAliasGovBr()` - Retorna o alias do provedor Gov.br
- `getProviderIdGovBr()` - Retorna o ID do Event Listener

## 🛠️ Instalação e Configuração no Keycloak

### 1. Build do Projeto

```bash
mvn clean package
```

### 2. Deploy no Keycloak

#### Keycloak Standalone
```bash
# Copie o JAR para o diretório de providers
cp target/keycloak-spi-selo-nivel-gov-br-1.0-SNAPSHOT.jar $KEYCLOAK_HOME/providers/

# Reinicie o Keycloak
$KEYCLOAK_HOME/bin/kc.sh start
```

#### Keycloak com Docker
```bash
# Copie o JAR para o container
docker cp target/keycloak-spi-selo-nivel-gov-br-1.0-SNAPSHOT.jar keycloak:/opt/keycloak/providers/

# Reinicie o container
docker restart keycloak
```

### 3. Configuração do Provedor Gov.br

1. **Acesse o Admin Console** do Keycloak
2. **Selecione seu Realm**
3. **Vá para Identity Providers**
4. **Adicione um novo provedor OIDC** com as seguintes configurações:

```
Alias: gov-br
Display Name: Gov.br
Authorization URL: https://sso.staging.acesso.gov.br/auth/realms/govbr/protocol/openid_connect/auth
Token URL: https://sso.staging.acesso.gov.br/auth/realms/govbr/protocol/openid_connect/token
Client ID: [seu-client-id]
Client Secret: [seu-client-secret]
```

### 4. Ativação do Event Listener

1. **Vá para Events → Config**
2. **Na seção Event Listeners**, adicione: `govbr-event-listener`
3. **Salve as configurações**

## 📊 Níveis e Selos Suportados

### Níveis de Autenticação
- **Bronze (1)** - Autenticação básica
- **Prata (2)** - Autenticação intermediária
- **Ouro (3)** - Autenticação avançada

### Selos de Confiabilidade

#### Bronze
- KBA Previdência (101)
- Cadastro Básico (201)
- Balcão SAT Previdência (501)
- Balcão Denatran (502)
- Balcão Correios (503)
- E outros...

#### Prata
- Servidor Público (301)
- Biovalid Facial (401)
- Internet Banking (diversos bancos: 602-627)

#### Ouro
- TSE Facial (701)
- Certificado Digital (801)
- CIN Facial (901)

## 🔍 Monitoramento

### Logs
O SPI gera logs detalhados que podem ser encontrados nos logs do Keycloak:

```bash
# Logs importantes para monitorar
tail -f $KEYCLOAK_HOME/data/log/keycloak.log | grep "GOVBR"
```

### Verificação dos Atributos
Após o login, verifique se os atributos foram criados:

1. **Admin Console → Users**
2. **Selecione o usuário**
3. **Aba Attributes**
4. **Procure por**: `nivel` e `selo`

## 🚨 Troubleshooting

### Problemas Comuns

1. **Event Listener não está sendo executado**
    - Verifique se o JAR está no diretório correto
    - Confirme que o Event Listener está ativado
    - Verifique os logs para erros de inicialização

2. **Atributos não estão sendo criados**
    - Verifique se o alias do provedor está correto (`gov-br`)
    - Confirme se as URLs das APIs estão acessíveis
    - Verifique se o token está sendo extraído corretamente

3. **Erro de conectividade com APIs**
    - Verifique se as URLs em `GovBrConfig` estão corretas
    - Confirme conectividade de rede
    - Verifique se o token de acesso é válido


---

**Versão:** 1.0-SNAPSHOT  
**Keycloak:** 24.0.0  
**Java:** 17+