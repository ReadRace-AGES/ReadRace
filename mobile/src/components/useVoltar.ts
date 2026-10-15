import { useRouter, type Href } from 'expo-router';
import { useRef } from 'react';

/**
 * O router enfileira a navegação: dois toques na seta voltariam duas telas e esvaziariam a pilha
 * (tela branca). A ref trava depois do primeiro toque; a tela sai da pilha em seguida.
 */
export function useVoltar(destinoSemHistorico: Href): () => void {
  const router = useRouter();
  const saindo = useRef(false);
  return () => {
    if (saindo.current) return;
    saindo.current = true;
    if (router.canGoBack()) {
      router.back();
    } else {
      router.replace(destinoSemHistorico);
    }
  };
}
