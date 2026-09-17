import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarForumDoClube, type ForumClube } from './api';

export type ForumCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: ForumClube };

export type ForumClubeState = {
  forum: ForumCarga;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o fórum do clube.';
}

/** Cabeçalho e posts chegam na mesma resposta: nenhum card sai meio renderizado. */
export function useForumClube(clubeId: string): ForumClubeState {
  const [forum, setForum] = useState<ForumCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setForum({ situacao: 'carregando' });
    buscarForumDoClube(clubeId, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setForum({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setForum({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { forum, recarregar };
}
