# EmptyState — #25

Componente de apresentação para listas vazias. A tela decide quando exibi-lo e fornece `message`, `icon` e, opcionalmente, `action`. Não faz requisições nem representa carregamento ou erro.

```tsx
<EmptyState
  icon={SearchIcon}
  message="Busque por leitores, comunidades ou livros"
/>
```

O renderizador do ícone recebe tamanho e cor do tema. Na Busca, use `SearchIcon` de `components/icons`, o mesmo do campo de busca. O ícone é decorativo para leitores de tela. O slot `action` aceita um `PrimaryButton` quando estiver disponível, sem acoplar o componente a ele. Sem ação, nenhum contêiner ou espaçamento de botão é criado.

O círculo usa `surfacePink`, o ícone `primarySoft` e o texto `textSecondary` com `textStyles.body`. Medidas usam os tokens existentes: ícone `sizes.icon`, preenchimento do círculo e distância até o texto `spacing[6]`, preenchimento externo `spacing[4]` e raio `radius.pill`. O PNG é referência do arranjo; não foram extraídas medidas dele.

O pai fornece a área livre da lista. Em `FlatList`, use `contentContainerStyle={{ flexGrow: 1 }}` e `ListEmptyComponent`. O componente cresce para centralizar o conteúdo, sem altura fixa ou truncamento. Quando a área for menor que a altura natural do conteúdo, inclusive com fontes ampliadas, o pai precisa permitir expansão ou rolagem para preservar a leitura.

## Validação manual

Em `mobile`, com Node 22:

```sh
npm ci
npm run web -- --port 8081
```

Abra `http://localhost:8081/teste`, ou “Abrir tela de teste” na aba Feed.

1. Em **Busca**, confira a lupa no círculo e a copy literal, sem quebra de linha manual.
2. Selecione **Outra lista**: mudam ícone e mensagem, mantendo o arranjo.
3. Selecione **Texto longo** e estreite a janela: texto completo, centralizado e sem rolagem horizontal.
4. Ative **Mostrar ação de teste**, toque no botão e confira o contador. Desative: botão e espaço correspondente desaparecem.
5. Ative **Área compacta** e repita com texto longo e fonte ampliada. A demonstração permite rolagem se necessário para não cortar o conteúdo.
6. Com leitor de tela, confira mensagem e botão; o ícone decorativo não deve ser anunciado.

A demonstração usa um botão local para exercitar o slot. A tela completa de Busca e seu campo pertencem à #44; nenhuma copy das demais telas foi definida aqui.
