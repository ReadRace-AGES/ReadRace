import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarFeedComunidades, type FeedComunidades } from './api';

/** As duas listas chegam juntas, então a tela nunca mostra uma seção com dado parcial. */
export type FeedCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: FeedComunidades };

export type FeedComunidadesState = {
  feed: FeedCarga;
  recarregar: () => void;
  atualizar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o feed de comunidades.';
}

export function useFeedComunidades(): FeedComunidadesState {
  const [feed, setFeed] = useState<FeedCarga>({ situacao: 'carregando' });
  // Silenciosa é a busca de fundo ao voltar para a aba: quem já está vendo o feed não perde o
  // conteúdo para o carregando, nem para um erro passageiro.
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });

  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const aplicar = (estado: FeedCarga, preservaConteudo: boolean) => {
      if (!ativo) return;
      setFeed((atual) =>
        preservaConteudo && atual.situacao === 'sucesso' ? atual : estado
      );
    };
    aplicar({ situacao: 'carregando' }, tentativa.silenciosa);
    buscarFeedComunidades(controller.signal)
      .then((dados) => aplicar({ situacao: 'sucesso', dados }, false))
      .catch((erro: unknown) =>
        aplicar(
          { situacao: 'erro', mensagem: mensagemDe(erro) },
          tentativa.silenciosa
        )
      );
    return () => {
      ativo = false;
      controller.abort();
    };
  }, [tentativa]);

  const recarregar = useCallback(
    () => setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: false })),
    []
  );
  const atualizar = useCallback(
    () => setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: true })),
    []
  );

  return { feed, recarregar, atualizar };
}
