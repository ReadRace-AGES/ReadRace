import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarFeedSocial, type FeedSocial } from './api';

export type FeedSocialCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: FeedSocial };

export type FeedSocialState = {
  social: FeedSocialCarga;
  recarregar: () => void;
  atualizar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar a leitura de quem você segue.';
}

/**
 * A aba Social só busca quando está selecionada (`ativa`): quem fica em Comunidades não paga a
 * chamada. Voltar para ela atualiza em silêncio, como o feed de comunidades ao voltar para a aba.
 */
export function useFeedSocial(ativa: boolean): FeedSocialState {
  const [social, setSocial] = useState<FeedSocialCarga>({
    situacao: 'carregando',
  });
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: true });

  useEffect(() => {
    if (!ativa) return;
    const controller = new AbortController();
    let ativo = true;
    const aplicar = (estado: FeedSocialCarga, preservaConteudo: boolean) => {
      if (!ativo) return;
      setSocial((atual) =>
        preservaConteudo && atual.situacao === 'sucesso' ? atual : estado
      );
    };
    aplicar({ situacao: 'carregando' }, tentativa.silenciosa);
    buscarFeedSocial(controller.signal)
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
  }, [ativa, tentativa]);

  const recarregar = useCallback(
    () =>
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: false })),
    []
  );
  const atualizar = useCallback(
    () =>
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: true })),
    []
  );

  return { social, recarregar, atualizar };
}
