import { useCallback, useRef, useState } from 'react';
import { router, useFocusEffect, type Href } from 'expo-router';

import { MeusLivrosView } from '@/features/biblioteca/MeusLivrosView';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';
import type { LivroBiblioteca } from '@/features/biblioteca/api';
import { RegistrarProgressoSheet } from '@/features/progresso/RegistrarProgressoSheet';

// O detalhe é empilhado dentro da aba, mantendo a barra de navegação.
function rotaDoDetalhe(livroId: string): Href {
  return `/biblioteca-livro/${encodeURIComponent(livroId)}` as Href;
}

export default function MeusLivrosScreen() {
  const biblioteca = useBiblioteca();
  const { atualizar } = biblioteca;
  const [registro, setRegistro] = useState<LivroBiblioteca | null>(null);
  useFocusEffect(useCallback(() => () => setRegistro(null), []));
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
    <>
      <MeusLivrosView
        {...biblioteca}
        onLivroPress={(livro) => router.push(rotaDoDetalhe(livro.livroId))}
        onAdicionarLivro={() => router.push('/adicionar-livro')}
        onRegistrarLeitura={setRegistro}
      />
      {registro && (
        <RegistrarProgressoSheet
          visible
          livroId={registro.livroId}
          totalPaginas={registro.totalPaginas}
          paginaAtual={registro.paginaAtual}
          paginaMaximaAlcancada={registro.paginaMaximaAlcancada}
          onClose={() => setRegistro(null)}
          onSuccess={atualizar}
        />
      )}
    </>
  );
}
