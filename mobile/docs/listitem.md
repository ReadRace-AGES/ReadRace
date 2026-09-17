# ListItem (#23)

Linha compartilhada para comunidades e livros, composta com Card, Avatar e
BookCover. Não busca dados, navega nem mantém seleção internamente.

## Contrato

| Prop          | Uso                                                                                                         |
| ------------- | ----------------------------------------------------------------------------------------------------------- |
| `variant`     | Obrigatória: `community` ou `book`                                                                          |
| `title`       | Obrigatório; uma linha com reticências                                                                      |
| `subtitle`    | Opcional; pronto para exibição, até duas linhas                                                             |
| `imageUrl`    | Opcional; usa o placeholder do Avatar/BookCover quando ausente ou inválida                                  |
| `onPress`     | Opcional; callback da linha                                                                                 |
| `memberCount` | Comunidade: metadado opcional, inclusive zero                                                               |
| `action`      | Comunidade: `{ label, onPress }`, opcional e independente do toque na linha                                 |
| `genre`       | Livro: etiqueta opcional                                                                                    |
| `selected`    | Livro: `true` destaca a linha e marca o círculo; `false` mostra o círculo vazio; omitido remove o indicador |

```tsx
<ListItem
  variant="book"
  title={livro.titulo}
  subtitle={livro.autor}
  imageUrl={livro.capaUrl}
  genre={livro.genero}
  selected={selecionado === livro.id}
  onPress={() => setSelecionado(livro.id)}
/>

<ListItem
  variant="community"
  title="Clube do Livro Quarta-Feira"
  subtitle="Harry Potter e a Pedra Filosofal - J.K Rowling"
  memberCount={43}
  onPress={abrirClube}
/>
```

O botão de ação é irmão da área principal: não há botões aninhados. Livros
interativos com seleção expõem papel `checkbox` e estado `checked`.
As cores de seleção usam `surfacePink` e `primary` do tema.
Card recebeu a prop opcional `surfaceStyle`, sem mudança nos valores padrão.

## Validação

Abra `/teste-listitem` no Expo para conferir comunidades, quatro livros com
seleção controlada, títulos longos, imagens ausentes ou quebradas e opcionais
omitidos. Confira também largura pequena, fonte ampliada e leitor de tela.

Execute `npm run test:listitem` e `npx tsc --noEmit` em `mobile`.
Os testes de renderização não substituem a validação visual e de toque nativo.

## Integração

Consumidores previstos: #34 (comunidades) e #40 (escolher livro do desafio).
A ação Participar é capacidade do componente, sem fluxo de adesão. Uma tela
que a usar antes de existir esse fluxo deve chamar o Toast de funcionalidade
em desenvolvimento. A demonstração apenas registra o callback recebido.
