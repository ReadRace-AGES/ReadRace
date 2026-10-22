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
O login no app agora é **nativo** (login por e-mail/senha e cadastro por e-mail falam direto com a
API do Cognito; o Google é federado pelo Cognito via OAuth/PKCE). Valores atuais (públicos, do
`mobile/.env.example`): domínio `https://us-east-2mevipejhy.auth.us-east-2.amazoncognito.com`,
client id `5f06kagodbku0mvveakmsim8qu`.
- [ ] Confirmar o **App client** e o **domínio** acima (conferir com o time se são os de produção).
- [ ] Garantir a **callback/redirect URL** `readrace://auth` cadastrada no app client (é o que o
      login Google usa; sem ela o Cognito recusa o retorno).
- [ ] Confirmar que **Authorization Code + PKCE** está habilitado e o provedor **Google** ativo.

### 2. Definir as variáveis do build (EAS Environment Variables — nuvem)
As variáveis `EXPO_PUBLIC_*` **não** ficam no `env` do `eas.json` (o inline não é embutido no
bundle de forma confiável). Cada perfil puxa as variáveis do seu **environment** no EAS. Defina-as
na nuvem com `eas env:create` (são públicas, use `--visibility plaintext`):

Para o environment `production` (perfil `production`, login ligado):
```
eas env:create --name EXPO_PUBLIC_API_URL --value "https://3-19-247-112.sslip.io" --environment production --visibility plaintext
eas env:create --name EXPO_PUBLIC_COGNITO_DOMAIN --value "https://us-east-2mevipejhy.auth.us-east-2.amazoncognito.com" --environment production --visibility plaintext
eas env:create --name EXPO_PUBLIC_COGNITO_CLIENT_ID --value "5f06kagodbku0mvveakmsim8qu" --environment production --visibility plaintext
```
Para o environment `preview` (perfil `preview`, login desligado — só a URL de produção):
```
eas env:create --name EXPO_PUBLIC_API_URL --value "https://3-19-247-112.sslip.io" --environment preview --visibility plaintext
```
Confira com `eas env:list --environment <env>`. Use `--force` para sobrescrever um valor existente.

> **Login só funciona em APK (development/production build), não no Expo Go** — o Google exige o
> retorno `readrace://auth`, que o Expo Go não fornece. O login por e-mail/senha e cadastro
> funcionam no Expo Go; só o Google exige o build.

### Perfis de build disponíveis (`eas.json`)
Cada perfil puxa as variáveis do seu **environment** no EAS (seção 2). Nenhum `env` inline no `eas.json`.

| Perfil | Environment | API | Login | Uso |
|---|---|---|---|---|
| `preview-local` | `development` | backend do PC do dev (ver abaixo) | desligado | testar com backend local |
| `preview` | `preview` | produção HTTPS | desligado (seed) | testar instalação contra produção |
| `production` | `production` | produção HTTPS | ligado (Cognito) | release final |

Comando: `eas build -p android --profile <perfil>`.

### Build local (`preview-local`) — o IP é de CADA dev
O perfil `preview-local` aponta o app para o backend rodando na máquina do próprio dev, então a URL
depende do **IP da máquina na rede Wi-Fi** e **muda de dev para dev e a cada troca de rede**. A URL
NÃO fica fixa no `eas.json`: ela vem da variável `EXPO_PUBLIC_API_URL` do environment `development`
no EAS (nuvem). Para gerar um `preview-local` seu:

1. Descubra o IP da sua máquina na Wi-Fi (Windows: `ipconfig` → "Endereço IPv4"; ex.: `192.168.0.104`).
2. Defina a variável no EAS (sobrescreve a existente):
   `eas env:create --name EXPO_PUBLIC_API_URL --value "http://SEU_IP:8080" --environment development --visibility plaintext --force`
   Confira com `eas env:list --environment development`.
3. Gere o APK: `eas build -p android --profile preview-local`.

Para rodar no **Expo Go** (sem gerar APK), crie um `mobile/.env` com `EXPO_PUBLIC_API_URL=http://SEU_IP:8080`
(o `.env` é lido só na sua máquina e é ignorado pelo git; não vale para o EAS Build, que usa a
variável da nuvem).

Pré-requisitos para o celular alcançar o backend local:
- Celular e PC na **mesma rede Wi-Fi** (redes de faculdade/empresa costumam ter **isolamento de
  clientes** e NÃO funcionam — use a produção HTTPS nesses casos).
- Liberar a **porta 8080 no firewall** do Windows (PowerShell admin):
  `New-NetFirewallRule -DisplayName "ReadRace API 8080 (dev)" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Any`
- O **cleartext HTTP** no APK é habilitado automaticamente pelo plugin `expo-build-properties` só
  quando a URL é `http://` (produção HTTPS fica sem cleartext). Não precisa configurar nada.

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

## Pré-requisito: merge `dev → main`
A produção sobe a partir da `main`, que ainda está na **sprint 2** (bem atrás da `dev`). Gerar o
APK de **produção** e virar `AUTH_MODO=cognito` **antes** do merge faria as telas novas chamarem
rotas que não existem em produção. Então os passos 3–6 (APK de produção, virar o modo, release)
dependem do merge `dev → main` acontecer primeiro.

## Fora de escopo (não fazer nesta task)
iOS e Play Store; atualização automática do app.

> Nota: as telas de login (#156), cadastro (#157) e login com Google (#158) **já foram
> implementadas** e estão na `dev` — o login no app é nativo. A observação original da issue (de
> que o primeiro APK usaria a página web do Cognito) não vale mais. Só a **recuperação de senha**
> ainda abre a página web do Cognito.
