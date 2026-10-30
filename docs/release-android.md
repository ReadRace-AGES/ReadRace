# Release do app Android + ligar o login em produção (#159)

> Documento operacional de trabalho. As mudanças de **código** (nome, package, versão, `eas.json`,
> README) já estão no repositório. Este checklist cobre os passos **manuais/de infraestrutura** que
> precisam ser feitos pelo time, fora do código — e que exigem acesso ao Expo/EAS, ao AWS Cognito e
> ao ambiente de produção.
>
> **Estado atual:** produção já está rodando com login (Cognito ligado por padrão desde a #214) e o
> APK de produção já foi gerado e validado. O que resta é a parte de divulgação: conta de
> demonstração e a distribuição do APK (hoje por link somente leitura no OneDrive).

## Resumo da task
Gerar um APK Android do ReadRace apontando para a API de produção com login ligado e distribuí-lo
(hoje por link somente leitura no OneDrive). Só Android (iOS fora de escopo).

> **Atualização (#214):** o login com Cognito virou o **padrão** em todos os ambientes
> (`AUTH_MODO` default = `cognito`). Produção já foi atualizada e **está rodando com login**. Logo,
> o antigo passo de "virar `seed` → `cognito` no mesmo dia, com backup antes" **não é mais
> necessário** — o modo já nasce ligado. O que sobra é gerar o APK de produção, validar e publicar.

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
Para o environment `preview` (perfil `preview`, aponta para a produção HTTPS; como a produção está
em `cognito`, o login fica ligado — para o `preview` ter sentido como "sem login" seria preciso uma
API de produção em `seed`, o que não é mais o caso):
```
eas env:create --name EXPO_PUBLIC_API_URL --value "https://3-19-247-112.sslip.io" --environment preview --visibility plaintext
eas env:create --name EXPO_PUBLIC_COGNITO_DOMAIN --value "https://us-east-2mevipejhy.auth.us-east-2.amazoncognito.com" --environment preview --visibility plaintext
eas env:create --name EXPO_PUBLIC_COGNITO_CLIENT_ID --value "5f06kagodbku0mvveakmsim8qu" --environment preview --visibility plaintext
```
Confira com `eas env:list --environment <env>`. Use `--force` para sobrescrever um valor existente.

> **Login só funciona em APK (development/production build), não no Expo Go** — o Google exige o
> retorno `readrace://auth`, que o Expo Go não fornece. O login por e-mail/senha e cadastro
> funcionam no Expo Go; só o Google exige o build.

### Perfis de build disponíveis (`eas.json`)
Cada perfil puxa as variáveis do seu **environment** no EAS (seção 2). Nenhum `env` inline no `eas.json`.

| Perfil | Environment | API | Login | Uso |
|---|---|---|---|---|
| `preview-local` | `development` | backend do PC do dev (ver abaixo) | depende do backend local | testar com backend local |
| `preview` | `preview` | produção HTTPS | ligado (Cognito) | testar instalação contra produção |
| `production` | `production` | produção HTTPS | ligado (Cognito) | release final |

Comando: `eas build -p android --profile <perfil>`.

> **Login no `preview-local`:** desde a #214 o backend sobe em `cognito` por **padrão**, então o
> `preview-local` também passa a exigir login. Para testar as telas **sem login** contra o backend
> local (todo request vira o usuário do seed), defina `AUTH_MODO=seed` no `.env` da raiz, reinicie
> a API e deixe as variáveis do Cognito vazias no `mobile/.env`.

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
O projeto EAS já existe (`@readrace/readrace`, `projectId` gravado no `app.config.js`) e já foram
gerados builds de produção. Para gerar/regenerar:
- [x] `eas-cli` instalado e `eas login` feito (conta `gugs123`).
- [x] `eas build -p android --profile production` — build de produção gerado e **funcionando**.
- [ ] Rebuildar sempre que entrarem mudanças relevantes na `dev` que o APK precise refletir
      (ex.: correção das capas em HTTPS da #216). Baixar o `.apk` pelo link do build.

### 4. Login em produção — JÁ LIGADO (#214)
Desde a #214, `AUTH_MODO` tem default `cognito` e os valores do Cognito (`issuer-uri`, `client-id`,
`domain`) vêm embutidos como padrão no `application.yaml` (são públicos, identificam o user pool).
Não há mais troca manual de `seed` → `cognito`: o modo já nasce ligado em todos os ambientes.

- [x] Produção atualizada e **rodando com login** (confirmado pelo time).
- [x] Validação: chamar uma rota de produto **sem token** → responde **401** (comportamento do
      modo `cognito`).

> Para **desligar** o login em algum ambiente (ex.: rodar sem internet), setar `AUTH_MODO=seed`
> explicitamente — é a exceção agora, não o padrão.

### 5. Conta de demonstração
- [ ] Criar a conta de demonstração no Cognito e **vinculá-la ao usuário de seed** (procedimento no
      README desde a #136). A vinculação ocorre por e-mail verificado no primeiro login.
- [ ] As credenciais da conta de demo **NÃO** vão no README público — ficam com o gestor e com quem
      apresenta.

### 6. Distribuir o APK
A distribuição atual é pelo **OneDrive**, com link **somente leitura**. O link muda a cada build
novo, então ele **não** fica fixo no README (o README orienta a pedir o link ao gestor).
- [ ] Baixar o `.apk` do build no EAS, subir no OneDrive e compartilhar o link como somente leitura.
- [ ] A cada novo build, gerar e divulgar um link novo.

> Alternativa futura, se quiserem um link fixo: publicar o `.apk` numa release do GitHub
> (`gh release create v0.1.0 caminho/para/readrace.apk --title "ReadRace 0.1.0" --notes "..."`) e
> apontar o README para ela.

## Validação final (num Android real, de alguém que não é dev do backend)
1. Baixar o APK pelo link compartilhado pelo time (OneDrive) e instalar → aparece o app **"ReadRace"** com o ícone do Figma.
2. Entrar com a **conta de demonstração** → ver os dados do usuário de seed (nível, Meus Livros,
   clubes, desafios).
3. Criar uma **conta nova** pelo app → perfil novo e **vazio** (não os dados da demo).
4. Chamar a API de produção **sem token** pelo terminal → **401**.
5. Conferir no README o link e as instruções.

## Pré-requisito: merge `dev → main` — RESOLVIDO
A produção sobe a partir da `main`. Esse merge **já aconteceu**: a produção foi atualizada com o
código novo e está rodando com login (Cognito). Logo, as telas novas já encontram as rotas
correspondentes na API de produção, e o APK de produção (gerado a partir da `feat/159` já integrada
com a `dev`) funciona de ponta a ponta. Os passos 3–6 deixam de depender de qualquer merge pendente.

## Fora de escopo (não fazer nesta task)
iOS e Play Store; atualização automática do app.

> Nota: as telas de login (#156), cadastro (#157) e login com Google (#158) **já foram
> implementadas** e estão na `dev` — o login no app é nativo. A observação original da issue (de
> que o primeiro APK usaria a página web do Cognito) não vale mais. Só a **recuperação de senha**
> ainda abre a página web do Cognito.
