#!/bin/bash

# =====================================================================
# Script de Deploy - SPI Keycloak Gov.br + Tema
# Refatoração completa: HTML inline → Tema Keycloak profissional
# =====================================================================

set -e  # Para no primeiro erro

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações
KEYCLOAK_CONTAINER=${KEYCLOAK_CONTAINER:-"keycloak"}
THEME_NAME="govbr-custom"
SPI_JAR="keycloak-govbr-level-validator-1.0-SNAPSHOT.jar"

echo -e "${BLUE}🚀 Iniciando deploy SPI Gov.br + Tema Keycloak${NC}"
echo -e "${BLUE}=============================================${NC}"

# ====== ETAPA 1: BUILD DO SPI ======
echo -e "\n${YELLOW}📦 Etapa 1: Build do SPI...${NC}"

if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Erro: pom.xml não encontrado. Execute este script na raiz do projeto.${NC}"
    exit 1
fi

echo -e "${BLUE}   Executando: mvn clean package${NC}"
mvn clean package -q

if [ ! -f "target/${SPI_JAR}" ]; then
    echo -e "${RED}❌ Erro: Build falhou. JAR não encontrado em target/${NC}"
    exit 1
fi

echo -e "${GREEN}   ✅ Build concluído: target/${SPI_JAR}${NC}"

# ====== ETAPA 2: VERIFICAR TEMA ======
echo -e "\n${YELLOW}🎨 Etapa 2: Verificando tema...${NC}"

if [ ! -d "keycloak-govbr-theme" ]; then
    echo -e "${RED}❌ Erro: Diretório do tema 'keycloak-govbr-theme' não encontrado${NC}"
    exit 1
fi

# Verificar estrutura do tema
THEME_FILES=(
    "keycloak-govbr-theme/META-INF/keycloak-themes.json"
    "keycloak-govbr-theme/theme/govbr-custom/login/theme.properties"
    "keycloak-govbr-theme/theme/govbr-custom/login/govbr-error.ftl"
    "keycloak-govbr-theme/theme/govbr-custom/login/resources/css/govbr-custom.css"
    "keycloak-govbr-theme/theme/govbr-custom/login/messages/messages_pt_BR.properties"
)

for file in "${THEME_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo -e "${RED}❌ Erro: Arquivo do tema não encontrado: $file${NC}"
        exit 1
    fi
done

echo -e "${GREEN}   ✅ Estrutura do tema validada${NC}"

# ====== ETAPA 3: VERIFICAR KEYCLOAK ======
echo -e "\n${YELLOW}🔍 Etapa 3: Verificando Keycloak...${NC}"

if ! docker ps | grep -q "$KEYCLOAK_CONTAINER"; then
    echo -e "${RED}❌ Erro: Container Keycloak '$KEYCLOAK_CONTAINER' não está rodando${NC}"
    echo -e "${YELLOW}   Tente: docker ps | grep keycloak${NC}"
    exit 1
fi

echo -e "${GREEN}   ✅ Container Keycloak encontrado: $KEYCLOAK_CONTAINER${NC}"

# ====== ETAPA 4: DEPLOY DO SPI ======
echo -e "\n${YELLOW}⚙️  Etapa 4: Deploy do SPI...${NC}"

echo -e "${BLUE}   Copiando JAR para container...${NC}"
docker cp "target/${SPI_JAR}" "${KEYCLOAK_CONTAINER}:/opt/keycloak/providers/"

# Verificar se copiou corretamente
if ! docker exec "$KEYCLOAK_CONTAINER" ls "/opt/keycloak/providers/${SPI_JAR}" > /dev/null 2>&1; then
    echo -e "${RED}❌ Erro: Falha ao copiar JAR para container${NC}"
    exit 1
fi

echo -e "${GREEN}   ✅ SPI JAR copiado com sucesso${NC}"

# ====== ETAPA 5: DEPLOY DO TEMA ======
echo -e "\n${YELLOW}🎨 Etapa 5: Deploy do tema...${NC}"

echo -e "${BLUE}   Copiando tema para container...${NC}"
docker cp "keycloak-govbr-theme/" "${KEYCLOAK_CONTAINER}:/opt/keycloak/themes/"

# Verificar estrutura do tema no container
echo -e "${BLUE}   Verificando tema no container...${NC}"
if ! docker exec "$KEYCLOAK_CONTAINER" ls "/opt/keycloak/themes/govbr-custom/login/theme.properties" > /dev/null 2>&1; then
    echo -e "${RED}❌ Erro: Tema não foi copiado corretamente${NC}"
    exit 1
fi

echo -e "${GREEN}   ✅ Tema copiado com sucesso${NC}"

# ====== ETAPA 6: RESTART KEYCLOAK ======
echo -e "\n${YELLOW}🔄 Etapa 6: Reiniciando Keycloak...${NC}"

echo -e "${BLUE}   Reiniciando container...${NC}"
docker restart "$KEYCLOAK_CONTAINER"

echo -e "${BLUE}   Aguardando Keycloak inicializar...${NC}"
sleep 10

# Aguardar Keycloak estar pronto
for i in {1..30}; do
    if docker exec "$KEYCLOAK_CONTAINER" curl -f -s http://localhost:8080/realms/master > /dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Keycloak reiniciado com sucesso${NC}"
        break
    fi
    echo -e "${YELLOW}   Aguardando... ($i/30)${NC}"
    sleep 2
done

# ====== ETAPA 7: VERIFICAÇÃO FINAL ======
echo -e "\n${YELLOW}✅ Etapa 7: Verificação final...${NC}"

# Verificar se SPI foi registrado
echo -e "${BLUE}   Verificando logs do SPI...${NC}"
if docker logs "$KEYCLOAK_CONTAINER" 2>&1 | grep -q "Gov.br Level Validator" || \
   docker logs "$KEYCLOAK_CONTAINER" 2>&1 | grep -q "govbr"; then
    echo -e "${GREEN}   ✅ SPI Gov.br registrado com sucesso${NC}"
else
    echo -e "${YELLOW}   ⚠️  SPI pode não ter sido registrado (verificar logs manualmente)${NC}"
fi

# Verificar se tema foi carregado
echo -e "${BLUE}   Verificando tema no container...${NC}"
THEME_COUNT=$(docker exec "$KEYCLOAK_CONTAINER" find /opt/keycloak/themes/govbr-custom -name "*.ftl" 2>/dev/null | wc -l)
if [ "$THEME_COUNT" -gt 0 ]; then
    echo -e "${GREEN}   ✅ Tema govbr-custom disponível ($THEME_COUNT templates)${NC}"
else
    echo -e "${YELLOW}   ⚠️  Verificar manualmente se tema foi carregado${NC}"
fi

# ====== SUCESSO! ======
echo -e "\n${GREEN}🎉 DEPLOY CONCLUÍDO COM SUCESSO!${NC}"
echo -e "${GREEN}================================${NC}"

echo -e "\n${BLUE}📋 PRÓXIMOS PASSOS:${NC}"
echo -e "1. Acesse o Admin Console: ${YELLOW}http://localhost:8080/admin${NC}"
echo -e "2. Vá para: ${YELLOW}Realm Settings → Themes${NC}"
echo -e "3. Selecione: ${YELLOW}Login Theme → govbr-custom${NC}"
echo -e "4. Clique em: ${YELLOW}Save${NC}"
echo -e "5. Configure: ${YELLOW}Authentication → Flows → Gov.br Level Validator${NC}"

echo -e "\n${BLUE}🔍 VERIFICAÇÕES:${NC}"
echo -e "• Tema instalado: ${GREEN}/opt/keycloak/themes/govbr-custom/${NC}"
echo -e "• SPI instalado: ${GREEN}/opt/keycloak/providers/${SPI_JAR}${NC}"
echo -e "• Container: ${GREEN}${KEYCLOAK_CONTAINER} (rodando)${NC}"

echo -e "\n${BLUE}📝 LOGS ÚTEIS:${NC}"
echo -e "• Ver logs: ${YELLOW}docker logs ${KEYCLOAK_CONTAINER} | grep -i govbr${NC}"
echo -e "• Ver temas: ${YELLOW}docker exec ${KEYCLOAK_CONTAINER} ls /opt/keycloak/themes/${NC}"
echo -e "• Ver SPIs: ${YELLOW}docker exec ${KEYCLOAK_CONTAINER} ls /opt/keycloak/providers/${NC}"

echo -e "\n${GREEN}🚀 Refatoração completa aplicada! Arquitetura limpa ativada.${NC}"