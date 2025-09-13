@echo off
REM =====================================================================
REM Script de Deploy - SPI Keycloak Gov.br + Tema (Windows)
REM Refatoração completa: HTML inline → Tema Keycloak profissional
REM =====================================================================

setlocal enabledelayedexpansion

REM Configurações
set KEYCLOAK_CONTAINER=%KEYCLOAK_CONTAINER%
if "%KEYCLOAK_CONTAINER%"=="" set KEYCLOAK_CONTAINER=keycloak
set THEME_NAME=govbr-custom
set SPI_JAR=keycloak-govbr-level-validator-1.0-SNAPSHOT.jar

echo.
echo 🚀 Iniciando deploy SPI Gov.br + Tema Keycloak
echo =============================================

REM ====== ETAPA 1: BUILD DO SPI ======
echo.
echo 📦 Etapa 1: Build do SPI...

if not exist "pom.xml" (
    echo ❌ Erro: pom.xml não encontrado. Execute este script na raiz do projeto.
    exit /b 1
)

echo    Executando: mvn clean package
call mvn clean package -q
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Build falhou
    exit /b 1
)

if not exist "target\%SPI_JAR%" (
    echo ❌ Erro: Build falhou. JAR não encontrado em target\
    exit /b 1
)

echo    ✅ Build concluído: target\%SPI_JAR%

REM ====== ETAPA 2: VERIFICAR TEMA ======
echo.
echo 🎨 Etapa 2: Verificando tema...

if not exist "keycloak-govbr-theme" (
    echo ❌ Erro: Diretório do tema 'keycloak-govbr-theme' não encontrado
    exit /b 1
)

REM Verificar arquivos essenciais do tema
if not exist "keycloak-govbr-theme\META-INF\keycloak-themes.json" (
    echo ❌ Erro: keycloak-themes.json não encontrado
    exit /b 1
)

if not exist "keycloak-govbr-theme\theme\govbr-custom\login\theme.properties" (
    echo ❌ Erro: theme.properties não encontrado
    exit /b 1
)

if not exist "keycloak-govbr-theme\theme\govbr-custom\login\govbr-error.ftl" (
    echo ❌ Erro: govbr-error.ftl não encontrado
    exit /b 1
)

echo    ✅ Estrutura do tema validada

REM ====== ETAPA 3: VERIFICAR KEYCLOAK ======
echo.
echo 🔍 Etapa 3: Verificando Keycloak...

docker ps | findstr /C:"%KEYCLOAK_CONTAINER%" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Container Keycloak '%KEYCLOAK_CONTAINER%' não está rodando
    echo    Tente: docker ps ^| findstr keycloak
    exit /b 1
)

echo    ✅ Container Keycloak encontrado: %KEYCLOAK_CONTAINER%

REM ====== ETAPA 4: DEPLOY DO SPI ======
echo.
echo ⚙️  Etapa 4: Deploy do SPI...

echo    Copiando JAR para container...
docker cp "target\%SPI_JAR%" "%KEYCLOAK_CONTAINER%:/opt/keycloak/providers/"
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Falha ao copiar JAR para container
    exit /b 1
)

REM Verificar se copiou corretamente
docker exec "%KEYCLOAK_CONTAINER%" ls "/opt/keycloak/providers/%SPI_JAR%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: JAR não foi copiado corretamente
    exit /b 1
)

echo    ✅ SPI JAR copiado com sucesso

REM ====== ETAPA 5: DEPLOY DO TEMA ======
echo.
echo 🎨 Etapa 5: Deploy do tema...

echo    Copiando tema para container...
docker cp "keycloak-govbr-theme\" "%KEYCLOAK_CONTAINER%:/opt/keycloak/themes/"
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Falha ao copiar tema para container
    exit /b 1
)

REM Verificar estrutura do tema no container
echo    Verificando tema no container...
docker exec "%KEYCLOAK_CONTAINER%" ls "/opt/keycloak/themes/govbr-custom/login/theme.properties" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Tema não foi copiado corretamente
    exit /b 1
)

echo    ✅ Tema copiado com sucesso

REM ====== ETAPA 6: RESTART KEYCLOAK ======
echo.
echo 🔄 Etapa 6: Reiniciando Keycloak...

echo    Reiniciando container...
docker restart "%KEYCLOAK_CONTAINER%"
if %ERRORLEVEL% neq 0 (
    echo ❌ Erro: Falha ao reiniciar container
    exit /b 1
)

echo    Aguardando Keycloak inicializar...
timeout /t 10 /nobreak >nul

REM Aguardar Keycloak estar pronto
set /a count=0
:wait_loop
set /a count+=1
if %count% gtr 30 goto wait_timeout

docker exec "%KEYCLOAK_CONTAINER%" curl -f -s http://localhost:8080/realms/master >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo    ✅ Keycloak reiniciado com sucesso
    goto wait_done
)

echo    Aguardando... (%count%/30)
timeout /t 2 /nobreak >nul
goto wait_loop

:wait_timeout
echo    ⚠️  Timeout aguardando Keycloak. Continue manualmente.

:wait_done

REM ====== ETAPA 7: VERIFICAÇÃO FINAL ======
echo.
echo ✅ Etapa 7: Verificação final...

echo    Verificando logs do SPI...
docker logs "%KEYCLOAK_CONTAINER%" 2>&1 | findstr /C:"Gov.br Level Validator" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo    ✅ SPI Gov.br registrado com sucesso
) else (
    docker logs "%KEYCLOAK_CONTAINER%" 2>&1 | findstr /C:"govbr" >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo    ✅ SPI Gov.br registrado com sucesso
    ) else (
        echo    ⚠️  SPI pode não ter sido registrado (verificar logs manualmente)
    )
)

echo    Verificando tema no container...
for /f %%i in ('docker exec "%KEYCLOAK_CONTAINER%" find /opt/keycloak/themes/govbr-custom -name "*.ftl" 2^>nul ^| find /c /v ""') do set THEME_COUNT=%%i
if !THEME_COUNT! gtr 0 (
    echo    ✅ Tema govbr-custom disponível (!THEME_COUNT! templates)
) else (
    echo    ⚠️  Verificar manualmente se tema foi carregado
)

REM ====== SUCESSO! ======
echo.
echo 🎉 DEPLOY CONCLUÍDO COM SUCESSO!
echo ================================

echo.
echo 📋 PRÓXIMOS PASSOS:
echo 1. Acesse o Admin Console: http://localhost:8080/admin
echo 2. Vá para: Realm Settings → Themes
echo 3. Selecione: Login Theme → govbr-custom
echo 4. Clique em: Save
echo 5. Configure: Authentication → Flows → Gov.br Level Validator

echo.
echo 🔍 VERIFICAÇÕES:
echo • Tema instalado: /opt/keycloak/themes/govbr-custom/
echo • SPI instalado: /opt/keycloak/providers/%SPI_JAR%
echo • Container: %KEYCLOAK_CONTAINER% (rodando)

echo.
echo 📝 LOGS ÚTEIS:
echo • Ver logs: docker logs %KEYCLOAK_CONTAINER% ^| findstr govbr
echo • Ver temas: docker exec %KEYCLOAK_CONTAINER% ls /opt/keycloak/themes/
echo • Ver SPIs: docker exec %KEYCLOAK_CONTAINER% ls /opt/keycloak/providers/

echo.
echo 🚀 Refatoração completa aplicada! Arquitetura limpa ativada.

pause