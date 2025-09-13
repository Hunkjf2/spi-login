<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false; section>
    <#if section = "header">
        ${msg("govbr.error.${errorType}.title")}
    <#elseif section = "form">
        <div class="govbr-error-container">
            <!-- Ícone de erro -->
            <div class="govbr-error-icon">🚫</div>
            
            <!-- Título dinâmico baseado no tipo de erro -->
            <h1 class="govbr-error-title">
                ${msg("govbr.error.${errorType}.title")}
            </h1>
            
            <!-- Mensagem principal -->
            <div class="govbr-error-message">
                <#if errorMessage?has_content>
                    ${errorMessage}
                <#elseif userLevel?has_content>
                    ${msg("govbr.error.${errorType}.message", userLevel)}
                <#else>
                    ${msg("govbr.error.${errorType}.message.generic")}
                </#if>
            </div>
            
            <!-- Detalhes específicos por tipo de erro -->
            <#if errorType == "INSUFFICIENT_LEVEL">
                <div class="govbr-error-details">
                    <h3>${msg("govbr.levels.title")}</h3>
                    <p><strong>Seu nível atual:</strong> <span class="govbr-level-badge ${userLevel?lower_case}">${userLevel!""}</span></p>
                    <p><strong>Níveis aceitos:</strong> ${acceptedLevelsText!""}</p>
                    
                    <ul class="govbr-levels-list">
                        <li class="govbr-level-item bronze <#if acceptedLevels?? && acceptedLevels?seq_contains('Bronze')>accepted</#if>">
                            <span class="govbr-level-badge bronze">Bronze</span>
                            ${msg("govbr.level.bronze")}
                            <#if acceptedLevels?? && acceptedLevels?seq_contains('Bronze')>
                                <strong class="accepted-badge">✅ Aceito</strong>
                            </#if>
                        </li>
                        <li class="govbr-level-item prata <#if acceptedLevels?? && acceptedLevels?seq_contains('Prata')>accepted</#if>">
                            <span class="govbr-level-badge prata">Prata</span>
                            ${msg("govbr.level.prata")}
                            <#if acceptedLevels?? && acceptedLevels?seq_contains('Prata')>
                                <strong class="accepted-badge">✅ Aceito</strong>
                            </#if>
                        </li>
                        <li class="govbr-level-item ouro <#if acceptedLevels?? && acceptedLevels?seq_contains('Ouro')>accepted</#if>">
                            <span class="govbr-level-badge ouro">Ouro</span>
                            ${msg("govbr.level.ouro")}
                            <#if acceptedLevels?? && acceptedLevels?seq_contains('Ouro')>
                                <strong class="accepted-badge">✅ Aceito</strong>
                            </#if>
                        </li>
                    </ul>
                </div>
            <#elseif errorType == "INVALID_TOKEN">
                <div class="govbr-error-details">
                    <h3>${msg("govbr.token.info.title")}</h3>
                    <p>${msg("govbr.token.info.description")}</p>
                    <ul>
                        <li>${msg("govbr.token.info.reason1")}</li>
                        <li>${msg("govbr.token.info.reason2")}</li>
                        <li>${msg("govbr.token.info.reason3")}</li>
                    </ul>
                </div>
            <#elseif errorType == "SERVICE_UNAVAILABLE">
                <div class="govbr-error-details">
                    <h3>${msg("govbr.service.info.title")}</h3>
                    <p>${msg("govbr.service.info.description")}</p>
                    <p><strong>${msg("govbr.service.info.suggestion")}</strong></p>
                </div>
            </#if>
            
            <!-- Botões de ação -->
            <div class="govbr-actions">
                <#if loginUrl?has_content>
                    <a href="javascript:void(0)" 
                       class="btn btn-primary govbr-btn-primary" 
                       onclick="forceLogoutAndLogin('${loginUrl}', this)">
                        ${msg("govbr.action.tryAgain")}
                        <span class="loading"></span>
                    </a>
                </#if>
            </div>
        </div>
        
        <!-- JavaScript inline mínimo -->
        <script>
            function showLoading(button) {
                const loading = button.querySelector('.loading');
                if (loading) {
                    loading.classList.add('show');
                }
            }

            function forceLogoutAndLogin(loginUrl, button) {
                // Mostrar loading e desabilitar botão
                showLoading(button);
                button.classList.add('processing');
                button.innerHTML = button.innerHTML.replace('Tentar Novamente', 'Limpando sessão...');
                
                // Limpar todas as sessões e cookies relacionados
                clearAllSessions();
                
                // Pequeno delay para garantir limpeza das sessões
                setTimeout(function() {
                    button.innerHTML = button.innerHTML.replace('Limpando sessão...', 'Redirecionando...');
                    
                    // Redirecionar para URL de login que força novo login
                    window.location.href = loginUrl;
                }, 800);
            }

            function clearAllSessions() {
                // Limpar localStorage
                try {
                    localStorage.clear();
                } catch(e) {
                    console.log('Erro ao limpar localStorage:', e);
                }

                // Limpar sessionStorage
                try {
                    sessionStorage.clear();
                } catch(e) {
                    console.log('Erro ao limpar sessionStorage:', e);
                }

                // Limpar cookies relacionados ao Keycloak e Gov.br
                const cookiesToClear = [
                    'KEYCLOAK_SESSION',
                    'KEYCLOAK_SESSION_LEGACY',
                    'KC_RESTART',
                    'KEYCLOAK_IDENTITY',
                    'KEYCLOAK_IDENTITY_LEGACY',
                    'KC_STATE_CHECKER',
                    'govbr-session',
                    'AUTH_SESSION_ID',
                    'AUTH_SESSION_ID_LEGACY'
                ];

                cookiesToClear.forEach(function(cookieName) {
                    // Limpar para o domínio atual
                    document.cookie = cookieName + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT; path=/;';
                    document.cookie = cookieName + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT; path=/; domain=' + window.location.hostname;
                    
                    // Limpar para subdomínios
                    const domain = window.location.hostname;
                    const parts = domain.split('.');
                    if (parts.length > 1) {
                        const parentDomain = '.' + parts.slice(-2).join('.');
                        document.cookie = cookieName + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT; path=/; domain=' + parentDomain;
                    }
                });

                console.log('🧹 Sessões e cookies limpos para forçar novo login');
            }

            // Auto-focus no botão principal após carregamento
            document.addEventListener('DOMContentLoaded', function() {
                setTimeout(function() {
                    const primaryBtn = document.querySelector('.govbr-btn-primary');
                    if (primaryBtn) {
                        primaryBtn.focus();
                    }
                }, 1000);

            });
        </script>
    </#if>
</@layout.registrationLayout>