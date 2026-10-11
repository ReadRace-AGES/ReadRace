# Release do app Android + ligar o login em produção (#159)

> Documento operacional de trabalho. As mudanças de **código** (nome, package, versão, `eas.json`,
> README) já estão no repositório. Este checklist cobre os passos **manuais/de infraestrutura** que
> precisam ser feitos pelo time, fora do código — e que exigem acesso ao Expo/EAS, ao AWS Cognito e
> ao ambiente de produção.

## Resumo da task
Gerar um APK Android do ReadRace apontando para a API de produção com login ligado, publicar num
link fixo (release do GitHub), e virar o modo de autenticação da API de `seed` para `cognito` —
tudo **no mesmo dia**, com **backup manual do banco antes**. Só Android (iOS fora de escopo).

## O que já está pronto no código
- `mobile/app.config.js`: `name: "ReadRace"`, `slug: "readrace"`, `version: "0.1.0"`,
  `android.package: "com.readrace.app"`, `android.versionCode: 1`, `scheme: "readrace"`.
- Ícone: `mobile/assets/images/icon.png` foi substituído pelo ícone do Figma (capivara + livro).
- `mobile/eas.json`: perfis de build (`preview`/`production`) gerando **APK** para distribuição
  interna, com as variáveis `EXPO_PUBLIC_*` a preencher.
- README atualizado com download/instalação/conta de demonstração.

## Ícone do Android — solução TEMPORÁRIA aplicada (revisar)
O launcher do Android mostra o **adaptive icon**, não o `icon.png`. Foi aplicada a **solução
temporária (Opção B)**:
- `android-icon-foreground.png` = o ícone do Figma **inteiro** (arte + fundo bordô embutido), já
  dentro da zona segura.
- `android-icon-background.png` = cor sólida **`#A1312F`** (a cor do fundo do ícone), e
  `adaptiveIcon.backgroundColor` foi alinhado para `#A1312F`.

**Por que é temporário:** o fundo fica `#A1312F`, não o `#732634` da identidade da marca; e o
`android-icon-monochrome.png` ainda é o antigo (não regenerado). O ideal (Opção A) é exportar do
Figma o **foreground isolado** (só a arte, com transparência, dentro da zona segura) e voltar o
`backgroundColor` para `colors.primary` (`#732634`), além de gerar o monochrome novo. Fica como
melhoria de design para uma próxima iteração.

## Checklist operacional

### 1. Cognito (AWS Console)
- [ ] Confirmar/criar o **App client** do ReadRace e anotar o **Client ID**.
- [ ] Confirmar o **domínio** do Cognito (ex.: `https://<dominio>.auth.us-east-1.amazoncognito.com`).
- [ ] Adicionar as **callback/redirect URLs** do app: `readrace://` (o `scheme` do app).
- [ ] Garantir que o fluxo de **Authorization Code** está habilitado para o app client.

### 2. Preencher as variáveis do build
No `mobile/eas.json` (perfil `production`) ou via **EAS Secrets**, definir os valores reais:
- [ ] `EXPO_PUBLIC_API_URL` → URL da API de produção.
- [ ] `EXPO_PUBLIC_COGNITO_DOMAIN` → domínio do Cognito.
- [ ] `EXPO_PUBLIC_COGNITO_CLIENT_ID` → Client ID do app client.
> Como são valores de ambiente de produção, o ideal é usar `eas secret:create` em vez de commitar
> no `eas.json`.

### 3. Gerar o APK (EAS Build)
Não há projeto EAS ainda — o primeiro build cria o projeto e grava o `projectId` no `app.config.js`.
- [ ] `npm i -g eas-cli` (se necessário) e `eas login`.
- [ ] `eas build -p android --profile production` (gera o APK na nuvem).
- [ ] Baixar o `.apk` gerado.

### 4. Virar o login em produção (no mesmo dia do release)
- [ ] **Backup manual do banco de produção ANTES** da troca (além do diário).
- [ ] Trocar `AUTH_MODO=seed` → `AUTH_MODO=cognito` na API de produção e definir
      `COGNITO_ISSUER_URI`, `COGNITO_CLIENT_ID`, `COGNITO_DOMAIN` (via SSM Parameter Store / deploy).
- [ ] Redeploy da API (produção sobe a partir da `main`).
- [ ] Validar: chamar uma rota de produto **sem token** → deve responder **401**.

### 5. Conta de demonstração
- [ ] Criar a conta de demonstração no Cognito e **vinculá-la ao usuário de seed** (procedimento no
      README desde a #136). A vinculação ocorre por e-mail verificado no primeiro login.
- [ ] As credenciais da conta de demo **NÃO** vão no README público — ficam com o gestor e com quem
      apresenta.

### 6. Publicar o APK
- [ ] Subir o `.apk` numa **release do GitHub** (link fixo citado no README).
      Ex.: `gh release create v0.1.0 caminho/para/readrace.apk --title "ReadRace 0.1.0" --notes "..."`.
- [ ] Conferir que o link no README aponta para essa release.

## Validação final (num Android real, de alguém que não é dev do backend)
1. Baixar o APK pelo link do README e instalar → aparece o app **"ReadRace"** com o ícone do Figma.
2. Entrar com a **conta de demonstração** → ver os dados do usuário de seed (nível, Meus Livros,
   clubes, desafios).
3. Criar uma **conta nova** pelo app → perfil novo e **vazio** (não os dados da demo).
4. Chamar a API de produção **sem token** pelo terminal → **401**.
5. Conferir no README o link e as instruções.

## Fora de escopo (não fazer nesta task)
iOS e Play Store; atualização automática do app; telas de login (#156) e cadastro (#157) dentro do
app — o primeiro APK usa a página do Cognito; login com Google (#158).
