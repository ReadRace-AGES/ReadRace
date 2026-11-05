import { useCallback, useRef } from 'react';
import { router, useFocusEffect, type Href } from 'expo-router';

import { MeusLivrosView } from '@/features/biblioteca/MeusLivrosView';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';

// O detalhe é empilhado dentro da aba, mantendo a barra de navegação.
function rotaDoDetalhe(livroId: string): Href {
  return `/biblioteca-livro/${encodeURIComponent(livroId)}` as Href;
}

export default function MeusLivrosScreen() {
  const biblioteca = useBiblioteca();
  const { atualizar } = biblioteca;
  const primeiroFoco = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (primeiroFoco.current) {
        primeiroFoco.current = false;
        return;
      }
      atualizar();
    }, [atualizar])
  );

  return (
    <MeusLivrosView
      {...biblioteca}
      onLivroPress={(livro) => router.push(rotaDoDetalhe(livro.livroId))}
      onAdicionarLivro={() => router.push('/adicionar-livro')}
    />
  );
}
