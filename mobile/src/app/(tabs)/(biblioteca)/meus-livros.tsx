import { router, type Href } from 'expo-router';

import { MeusLivrosView } from '@/features/biblioteca/MeusLivrosView';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';

// O detalhe é empilhado dentro da aba, mantendo a barra de navegação.
function rotaDoDetalhe(livroId: string): Href {
  return `/livro/${livroId}` as Href;
}

export default function MeusLivrosScreen() {
  const biblioteca = useBiblioteca();

  return (
    <MeusLivrosView
      {...biblioteca}
      onLivroPress={(livro) => router.push(rotaDoDetalhe(livro.livroId))}
    />
  );
}
