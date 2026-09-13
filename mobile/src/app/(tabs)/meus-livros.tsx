import { router, type Href } from 'expo-router';

import { MeusLivrosView } from '@/features/biblioteca/MeusLivrosView';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';

// TODO(#32): a rota do Detalhe do livro ainda não existe; o `Href` é o contrato esperado.
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
