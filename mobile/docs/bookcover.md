# BookCover (#19)

```tsx
import { BookCover } from '@/components/BookCover';

<BookCover
  size="grid"
  source={{ uri: livro.capaUrl }}
  accessibilityLabel={`Capa de ${livro.titulo}`}
  onPress={() => abrirLivro(livro.id)}
/>;

<BookCover variant="add-favorite" onPress={showToast} />;
```

`source` aceita URL, objeto com `uri` do Expo ou asset local via `require`.
Ausência, URL vazia e erro de carregamento mostram apenas o bloco neutro, sem
ícone ou texto. O tamanho é reservado antes da imagem chegar. Trocar a fonte
permite recuperar uma imagem que falhou, inclusive em linhas reutilizadas.

Os tamanhos `thumbnail`, `grid` (padrão) e `featured` estão centralizados em
`theme.bookCover`. São composições da escala já existente: 40x50, 80x100 e
120x150, com `radius.md`, sem novas medições dos frames tardios do Figma.
São uma reconciliação de implementação, não medidas extraídas do design;
a fidelidade das dimensões precisa de ratificação do tema. Nenhuma sombra nova
foi introduzida.

O slot de adicionar sempre usa o tamanho de grade e exige `onPress`. A tela o
coloca por último e passa o disparador do Toast #29. BookCover não navega, não
altera favoritos e não importa um provider de notificações. A rota `/teste`
inclui um aviso local para validar o toque enquanto o Toast compartilhado está
em PR; essa demonstração não substitui a entrega da #29.

## Como testar

Na raiz do repositório, com Node 22:

```sh
cd mobile
npm ci
npx tsc --noEmit
npm run web
```

Abra `/teste` no endereço mostrado pelo Expo, ou use **Abrir tela de teste** na
aba Feed. Não precisa de backend nem banco. Se o Node global ainda for 18:

```sh
npm exec --yes --package=node@22 -- node node_modules/expo/bin/cli start --web
```

1. Confira a mesma capa nos três tamanhos, sem distorção.
2. Toque nas capas: o contador deve aumentar, sem navegar.
3. Confira os dois blocos neutros: sem capa e URL inválida, com mesmo tamanho.
4. Toque em **Trocar por capa válida** e verifique a recuperação da imagem.
5. Com rede lenta, confira que a chegada da capa não desloca os demais itens.
6. Toque em **adicionar favorito +**: aparece o aviso por 2,5 segundos; nenhum
   livro é adicionado. Toques repetidos mantêm um único aviso.
7. Saia da tela com o aviso visível: ele não deve acompanhar a navegação.
8. Repita em dispositivo e com leitor de tela para conferir os rótulos e toques.

## Docker (PowerShell, raiz do repositório)

```powershell
docker run --rm -it -p 8081:8081 -e BROWSER=none -v "${PWD}/mobile:/app" -v readrace-mobile-node-modules:/app/node_modules -w /app node:22-bookworm sh -c "npm ci && npx expo start --web --host lan --port 8081"
```

Abra `http://localhost:8081/teste`. O Compose atual sobe apenas banco e backend.
