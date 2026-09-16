# PrimaryButton — #16

Botão de ação do app inteiro: preenchido, contorno, com ícone opcional à esquerda, desabilitado e carregando. O rótulo vem sempre de quem usa — o componente não tem copy própria.

```tsx
<PrimaryButton label="Criar Grupo" icon={PlusIcon} onPress={criarGrupo} />
<PrimaryButton label="Cancelar" variant="outline" onPress={cancelar} />
<PrimaryButton label="Enviar Desafio" icon={IconeDeExemplo} loading={enviando} onPress={enviarDesafio} />
```

O renderizador de `icon` recebe tamanho e cor do tema, no mesmo formato do `icon` do `EmptyState`. A largura acompanha o bloco em que o botão é usado — o componente não define a sua.

**Carregando é um padrão único do app inteiro** (decisão 6, `sprint-1.md` #16): enquanto `loading` é `true`, o rótulo (e o ícone, se houver) dá lugar a um `ActivityIndicator` e o botão fica desabilitado — o `Pressable` interno bloqueia o toque, então nenhuma tela precisa guardar contra duplo disparo. Nenhuma tela deve desenhar o seu próprio estado de carregando. O Figma não desenha esse estado nem o desabilitado (ver `README` § Tema do app e a issue #16); a aparência de ambos sai só dos tokens: fundo `surfaceDisabled`, texto/ícone `textMuted`.

Cores usam só tokens existentes, nunca um valor medido direto no Figma (regra do próprio `tokens.js`: frames com `node-id >= 2011` são "feitos às pressas" e não servem de fonte de cor). Preenchido é `primary`/`textInverse` com `shadows.button` — confirmado pixel a pixel contra o frame válido `3-6` (`X0 Y2 blur 6.4 spread 0 #000000 25%`, o mesmo shadow do token). Contorno é `surface`, sem sombra, e tem duas leituras porque os dois exemplos do Figma divergem entre si (ambos em frames "feitos às pressas": `2060-1910` e `2011-1538`):

| Exemplo Figma | Medido no Figma | Token usado |
|---|---|---|
| "Cancelar", sem ícone (`2060-1910`) | borda `#877273`, texto `#22191A` | borda `borderStrong`, texto `text` |
| "Revanche", com ícone (`2011-1538`) | borda e texto/ícone `#560F1F` | borda, texto e ícone `primary` |

Ou seja: contorno **sem ícone** fica neutro (`borderStrong`/`text`); contorno **com ícone** fica na cor da marca (`primary`). Nenhum dos dois valores medidos virou token novo — cada um foi para o token válido mais próximo, para não violar a regra do tema.

## Validação manual

Em `mobile`, com Node 22:

```sh
npm ci
npm run web -- --port 8081
```

Abra `http://localhost:8081/teste`.

1. Em **PrimaryButton — #16**, compare as cinco variantes com as telas do Design da issue #16 (formato pílula, ícone à esquerda). No contorno, confira que "Cancelar" (sem ícone) sai neutro e "Revanche" (com ícone) sai na cor da marca — são leituras diferentes de propósito, ver tabela acima.
2. Toque no preenchido e no contorno algumas vezes: o contador de toques sobe a cada toque.
3. Toque no botão desabilitado (estático) e no botão carregando (estático): o contador correspondente não sobe.
4. Em **Rótulo longo demais para a largura**, confirme que o texto quebra em linhas, o botão cresce e o ícone continua inteiro. Repita com fonte ampliada: a altura mínima vem de `sizes.buttonHeight`, sem recortar o rótulo.
5. Em **Carregando — chamada simulada**, toque no botão: ele fica desabilitado e mostra o indicador por 2s. Toque várias vezes durante esse período — nenhuma chamada nova é disparada (o próprio botão ignora o toque). Ao terminar, o rótulo volta e o contador de chamadas concluídas sobe.
6. Ative **Chamada termina com erro** e repita o passo 5: o botão sai de carregando e volta a aceitar toque, mas o contador de sucesso não sobe — o tratamento do erro em si é de quem usa o botão, fora do escopo desta task.

O ícone de demonstração (`+`) é local à tela de teste; cada tela de produto passa o ícone que fizer sentido para sua ação.
