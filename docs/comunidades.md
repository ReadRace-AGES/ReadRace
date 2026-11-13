# Criação de comunidades

O Feed oferece **Criar comunidade** em **Minhas comunidades**. O formulário tem prévia,
nome obrigatório, descrição opcional e capa por URL HTTPS. Ao concluir, abre a comunidade;
o criador já pode publicar. A descrição aparece no perfil e o Feed é atualizado.

## Contrato e integridade

`POST /api/comunidades` recebe:

```json
{
  "nome": "Entre páginas e cafés",
  "descricao": "Conversas sobre livros e boas histórias.",
  "imagemUrl": null
}
```

Retorna **201**, cabeçalho `Location: /api/comunidades/{id}` e
`{ id, nome, descricao, imagemUrl, seguidores: 1, sigo: true }`.

- Nome: obrigatório, até 120 unidades UTF-16, sem caracteres de controle.
- Descrição: opcional, até 2.000 unidades UTF-16.
- Capa: opcional, URL HTTPS absoluta de até 2.048 caracteres, sem credenciais.
- Espaços externos são removidos; campos opcionais vazios viram `null`.
- A sessão define o administrador; dados de identidade enviados no corpo não definem autoria.
- O usuário deve estar ativo. Um lock no usuário protege o vínculo durante a criação.
- Comunidade e administrador são persistidos na mesma transação. Falha no vínculo reverte ambos.
- O formulário impede envios simultâneos, preserva os campos após erro e cancela respostas antigas.
- Nomes iguais são permitidos; cada comunidade tem UUID próprio. Não há garantia de idempotência
  entre pedidos HTTP independentes. O bloqueio de duplicidade de envio é local ao formulário.
- O backend armazena a URL e não baixa nem valida o conteúdo remoto. Uma imagem indisponível
  usa a identidade visual padrão no formulário e na página.

O schema existente atende ao fluxo, sem migration. A regra de proteção ao último administrador
permanece: ele não pode sair. Transferência de administração e edição não fazem parte desta entrega.

## Navegação

Comunidades abertas no Feed usam `/feed-comunidade/{id}`. A Busca mantém `/comunidade/{id}`.
Os endereços distintos preservam a aba de origem após recarregar a página no navegador,
evitando a escolha ambígua entre rotas com o mesmo caminho no Expo Router.

## Validação

```powershell
cd mobile
npm test
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:E2E_PG_BIN = 'C:/Program Files/PostgreSQL/17/bin'
npm run test:e2e -- --native --grep 'Criação de comunidade|Comunidade sugerida' --retries=0
```

O runner cria um cluster temporário, restaura o banco antes de cada teste e encerra o ambiente.
As chamadas de criação e publicação usam API e banco reais. Apenas a falha simulada de serviço
e as imagens de teste são interceptadas. As capturas ficam em `mobile/e2e-artifacts/comunidade-*.png`
e também são anexadas ao relatório Playwright.

Os testes Java de criação cobrem limites, dados inválidos, normalização, autoria,
integração com Feed/Busca, usuário excluído, proteção ao administrador e rollback.
O teste de Cognito verifica falta de token e tentativa de forjar o criador.

Validação local em 12/11/2026: **265 testes Java selecionados**, **371 testes do aplicativo**
e **4 E2E de comunidades** passaram. O E2E cobre larguras de 320 e 390 pixels, recarga da página,
publicação, persistência, imagem indisponível e recuperação após erro. O login real no Cognito
e os comportamentos nativos Android/iOS não são comprovados pelo E2E seed no Chromium.
A exportação web passou. O `tsc --noEmit` global ainda encontra erros em componentes preexistentes
(barra de navegação, ranking, telas de acesso, chat, comentários e perfil), fora desta alteração;
o diagnóstico está em `mobile/e2e-artifacts/typescript.log`.

Por padrão, os testes Java continuam usando Testcontainers/PostgreSQL 16.
Para usar um **banco descartável previamente criado** sem Docker, definir `DB_URL`, `DB_USER`
e `DB_PASSWORD` explicitamente e passar `-Dreadrace.test.postgres-local=true` ao Maven.
Esse modo não cria nem remove o banco e executa migrations/seeds; nunca apontá-lo para o banco
de desenvolvimento ou produção. A validação local desta entrega usou JDK 21 e um cluster
PostgreSQL 17 independente, encerrado ao término.

## Sugestão para upload e processamento de fotos

A entrega aceita links. Para permitir escolher uma foto do dispositivo, a evolução sugerida é:

1. Selecionar e recortar no aplicativo, com prévia da área visível da capa.
2. Pedir à API autenticada uma autorização curta de upload para uma chave aleatória própria do
   usuário. Enviar o arquivo diretamente a uma área privada de armazenamento.
3. Processar em segundo plano: limitar bytes e pixels, identificar o formato real, rejeitar arquivos
   inválidos, corrigir a orientação e remover metadados, inclusive localização.
4. Gerar capa em WebP de aproximadamente 1.200 × 600 e miniatura quadrada de 160 × 160.
   Esses tamanhos são uma proposta inicial: ajustar ao recorte escolhido e medir qualidade/peso.
5. Publicar apenas as variantes processadas por CDN. Persistir a chave definitiva na comunidade,
   com verificação de propriedade; descartar uploads abandonados e manter a capa padrão durante
   o processamento ou em caso de falha.

Uma opção compatível com a infraestrutura AWS do projeto é S3 com URL pré-assinada e um worker
ou Lambda para transformar as imagens. A [documentação do S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
descreve o upload autorizado sem entregar credenciais AWS ao cliente. Usar chaves únicas evita
sobrescrever objetos existentes.

Se o processamento usar Node, Sharp oferece [correção de orientação](https://sharp.pixelplumbing.com/api-operation/),
[redimensionamento e recorte](https://sharp.pixelplumbing.com/api-resize/) e
[saída WebP com remoção padrão de metadados](https://sharp.pixelplumbing.com/api-output/).
Isso seria um serviço de processamento separado; Sharp não foi adicionado ao aplicativo Expo
nem ao backend Java nesta entrega.
