import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarClube, type Clube } from './api';

export type ClubeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Clube };

export type ClubeState = {
  clube: ClubeCarga;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o clube.';
}

/**
 * Cabeçalho e ranking chegam na mesma resposta, então a tela nunca mostra meia página.
 *
 * Recarregar depois de registrar leitura é o comportamento pedido pela #35: o ranking volta
 * igual porque nenhuma ação desta sprint cria `Pontos`.
 */
export function useClube(clubeId: string): ClubeState {
  const [clube, setClube] = useState<ClubeCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setClube({ situacao: 'carregando' });
    buscarClube(clubeId, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setClube({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setClube({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { clube, recarregar };
}
