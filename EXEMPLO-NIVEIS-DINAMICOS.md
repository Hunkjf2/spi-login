# 🎯 Validação Dinâmica de Níveis Gov.br

## 📋 Como Configurar Níveis Aceitos

A validação agora é **totalmente dinâmica** baseada no array `ACCEPTED_LEVELS` em `GovBrConfig.java`.

---

## ⚙️ Configurações Possíveis

### **Exemplo 1: Apenas Bronze**
```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Bronze"};
```

**Resultado**:
- ✅ Usuários Bronze: **Acesso liberado**
- ❌ Usuários Prata/Ouro: **Erro de nível insuficiente**
- 📝 Mensagem: *"É necessário possuir nível Bronze no Gov.br"*

### **Exemplo 2: Bronze e Prata**
```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Bronze", "Prata"};
```

**Resultado**:
- ✅ Usuários Bronze/Prata: **Acesso liberado**
- ❌ Usuários Ouro: **Erro de nível insuficiente**
- 📝 Mensagem: *"É necessário possuir níveis Bronze ou Prata no Gov.br"*

### **Exemplo 3: Apenas Ouro (Mais Restritivo)**
```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Ouro"};
```

**Resultado**:
- ❌ Usuários Bronze/Prata: **Erro de nível insuficiente**
- ✅ Usuários Ouro: **Acesso liberado**
- 📝 Mensagem: *"É necessário possuir nível Ouro no Gov.br"*

### **Exemplo 4: Todos os Níveis**
```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Bronze", "Prata", "Ouro"};
```

**Resultado**:
- ✅ Todos os usuários: **Acesso liberado**
- 📝 Mensagem: *"É necessário possuir níveis Bronze, Prata ou Ouro no Gov.br"*

---

## 🎨 Interface Dinâmica

### **Visual da Página de Erro (Bronze aceito)**

```
🚫 Nível de Autenticação Insuficiente

Seu nível atual não permite acesso a este sistema.

┌─────────────────────────────────────────┐
│ Seu nível atual: [Prata]                │
│ Níveis aceitos: nível Bronze            │
│                                         │
│ ✅ Bronze: Cadastro básico - ✅ Aceito  │
│ ❌ Prata: Dados bancários validados     │
│ ❌ Ouro: Validação presencial           │
└─────────────────────────────────────────┘

[🔄 Tentar Novamente] [🚪 Sair]
```

### **Visual da Página de Erro (Bronze e Ouro aceitos)**

```
🚫 Nível de Autenticação Insuficiente

Seu nível atual não permite acesso a este sistema.

┌─────────────────────────────────────────┐
│ Seu nível atual: [Prata]                │
│ Níveis aceitos: níveis Bronze ou Ouro   │
│                                         │
│ ✅ Bronze: Cadastro básico - ✅ Aceito  │
│ ❌ Prata: Dados bancários validados     │
│ ✅ Ouro: Validação presencial - ✅ Aceito│
└─────────────────────────────────────────┘

[🔄 Tentar Novamente] [🚪 Sair]
```

---

## 🔧 Implementação Técnica

### **1. Geração Automática de Mensagens**

```java
// GovBrThemeErrorHandler.java:214-237
private static String gerarTextoNiveisAceitos() {
    if (ACCEPTED_LEVELS.length == 1) {
        return "nível " + ACCEPTED_LEVELS[0];
    }
    if (ACCEPTED_LEVELS.length == 2) {
        return "níveis " + ACCEPTED_LEVELS[0] + " ou " + ACCEPTED_LEVELS[1];
    }
    // 3+ níveis: "níveis Bronze, Prata ou Ouro"
}
```

### **2. Validação Dinâmica**

```java
// GovBrThemeErrorHandler.java:205-209
private static boolean isNivelAceito(String userLevel) {
    return Arrays.stream(ACCEPTED_LEVELS)
            .anyMatch(level -> level.equalsIgnoreCase(userLevel.trim()));
}
```

### **3. Template Inteligente**

```html
<!-- govbr-error.ftl:30-31 -->
<p><strong>Seu nível atual:</strong> <span class="govbr-level-badge ${userLevel?lower_case}">${userLevel!""}</span></p>
<p><strong>Níveis aceitos:</strong> ${acceptedLevelsText!""}</p>

<!-- Destaque visual para níveis aceitos -->
<#if acceptedLevels?? && acceptedLevels?seq_contains('Bronze')>
    <strong class="accepted-badge">✅ Aceito</strong>
</#if>
```

---

## 🧪 Casos de Teste

### **Teste 1: Configuração Bronze Apenas**

```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Bronze"};
```

**Cenários**:
- 🔵 Login Bronze → ✅ **Sucesso**
- 🟡 Login Prata → ❌ **Erro: "É necessário possuir nível Bronze"**
- 🟠 Login Ouro → ❌ **Erro: "É necessário possuir nível Bronze"**

### **Teste 2: Configuração Múltipla**

```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {"Bronze", "Ouro"};
```

**Cenários**:
- 🔵 Login Bronze → ✅ **Sucesso**
- 🟡 Login Prata → ❌ **Erro: "É necessário possuir níveis Bronze ou Ouro"**
- 🟠 Login Ouro → ✅ **Sucesso**

### **Teste 3: Array Vazio (Edge Case)**

```java
// GovBrConfig.java
public static final String[] ACCEPTED_LEVELS = {};
```

**Resultado**:
- 🚫 Todos os logins → ❌ **Erro: "Nenhum nível configurado"**

---

## 📊 Logs de Debug

### **Logs Gerados Automaticamente**

```bash
# Login com Bronze (aceito)
✅ Login aprovado - Usuário: joao@gov.br - Nível: Bronze

# Login com Prata (não aceito) 
❌ Login rejeitado - Usuário: maria@gov.br - Erro: Nível insuficiente
📋 Níveis aceitos: [Bronze] - Nível usuário: Prata

# Geração de mensagem dinâmica
🎯 Texto gerado: "É necessário possuir nível Bronze no Gov.br"
```

---

## 🔄 Migração de Configurações

### **Cenário Comum: Sistema Corporativo**

**ANTES**: Hardcoded para Ouro apenas
```java
// Código antigo (fixo)
if (!userLevel.equals("Ouro")) {
    return erro("Necessário nível Ouro");
}
```

**DEPOIS**: Configurável dinamicamente
```java
// Código novo (flexível)
public static final String[] ACCEPTED_LEVELS = {"Prata", "Ouro"}; // Corporativo
// ou
public static final String[] ACCEPTED_LEVELS = {"Bronze"}; // Público geral
```

### **Benefícios da Migração**

| **Aspecto** | **ANTES** | **DEPOIS** |
|-------------|-----------|------------|
| **Flexibilidade** | ❌ Fixo no código | ✅ Configurável |
| **Manutenção** | ❌ Recompilação | ✅ Apenas config |
| **Mensagens** | ❌ Hardcoded | ✅ Dinâmicas |
| **Interface** | ❌ Estática | ✅ Adaptativa |
| **Deploy** | ❌ Build novo | ✅ Config apenas |

---

## 🎯 Exemplos Práticos de Uso

### **Sistema Público (Bronze)**
```java
public static final String[] ACCEPTED_LEVELS = {"Bronze"};
// Acesso básico para cidadãos em geral
```

### **Sistema Corporativo (Prata+)**
```java
public static final String[] ACCEPTED_LEVELS = {"Prata", "Ouro"};
// Acesso para empresas e pessoas jurídicas
```

### **Sistema Crítico (Ouro)**
```java
public static final String[] ACCEPTED_LEVELS = {"Ouro"};
// Máxima segurança para dados sensíveis
```

### **Sistema de Desenvolvimento (Todos)**
```java
public static final String[] ACCEPTED_LEVELS = {"Bronze", "Prata", "Ouro"};
// Ambiente de testes com acesso liberado
```

---

## ✅ Resultado Final

### **🎯 Validação Totalmente Dinâmica Implementada**

- ✅ **Configuração simples**: Apenas alterar array `ACCEPTED_LEVELS`
- ✅ **Mensagens automáticas**: Geradas conforme configuração
- ✅ **Interface adaptativa**: Destaca níveis aceitos visualmente
- ✅ **Zero recompilação**: Mudança de config não precisa rebuild
- ✅ **Compatibilidade total**: Funciona com configuração atual

**🎉 Sistema agora aceita qualquer combinação de níveis Gov.br configurada em `ACCEPTED_LEVELS`!**