import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarRankingClube, type RankingClube } from './api';

export type RankingClubeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: RankingClube };

export type RankingClubeState = {
  ranking: RankingClubeCarga;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o ranking.';
}

export function useRankingClube(clubeId: string): RankingClubeState {
  const [ranking, setRanking] = useState<RankingClubeCarga>({
    situacao: 'carregando',
  });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setRanking({ situacao: 'carregando' });
    buscarRankingClube(clubeId, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setRanking({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setRanking({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { ranking, recarregar };
}
