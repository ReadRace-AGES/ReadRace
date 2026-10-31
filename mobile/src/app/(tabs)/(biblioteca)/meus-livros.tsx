import { useCallback, useEffect, useRef } from 'react';
import {
  router,
  useFocusEffect,
  useLocalSearchParams,
  type Href,
} from 'expo-router';

import { MeusLivrosView } from '@/features/biblioteca/MeusLivrosView';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';

// O detalhe é empilhado dentro da aba, mantendo a barra de navegação.
function rotaDoDetalhe(livroId: string): Href {
  return `/biblioteca-livro/${encodeURIComponent(livroId)}` as Href;
}

export default function MeusLivrosScreen() {
  const biblioteca = useBiblioteca();
  const firstFocus = useRef(true);
  // "livroAdicionado" chega só quando /adicionar-livro volta por aqui (#155); não é foco de
  // tela, então não refaz a chamada à toa (ver comentário em useBiblioteca sobre "Ver mais").
  const { livroAdicionado } = useLocalSearchParams<{
    livroAdicionado?: string;
  }>();

  useEffect(() => {
    if (livroAdicionado) biblioteca.recarregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [livroAdicionado]);

  useFocusEffect(
    useCallback(() => {
      if (firstFocus.current) {
        firstFocus.current = false;
        return;
      }
      biblioteca.recarregar();
    }, [biblioteca.recarregar])
  );

  return (
    <MeusLivrosView
      {...biblioteca}
      onLivroPress={(livro) => router.push(rotaDoDetalhe(livro.livroId))}
      onAdicionarLivro={() => router.push('/adicionar-livro')}
    />
  );
}
