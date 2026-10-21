import { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { ApiError } from '@/api/client';

import { buscarRankingClube, type RankingClube } from './api';

export type RankingClubeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: RankingClube; carregandoMais: boolean };

export type RankingClubeState = {
  ranking: RankingClubeCarga;
  recarregar: () => void;
  carregarMais: () => void;
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
  const requisicao = useRef<AbortController | null>(null);
  const requisicaoMais = useRef<AbortController | null>(null);

  const recarregar = useCallback(() => {
    requisicao.current?.abort();
    requisicaoMais.current?.abort();
    const controller = new AbortController();
    requisicao.current = controller;
    setRanking((atual) =>
      atual.situacao === 'sucesso' ? atual : { situacao: 'carregando' }
    );
    buscarRankingClube(clubeId, null, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setRanking({ situacao: 'sucesso', dados, carregandoMais: false });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setRanking({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
  }, [clubeId]);

  const carregarMais = useCallback(() => {
    if (
      ranking.situacao !== 'sucesso' ||
      ranking.carregandoMais ||
      ranking.dados.proximoCursor === null
    ) {
      return;
    }
    const controller = new AbortController();
    requisicaoMais.current = controller;
    setRanking({ ...ranking, carregandoMais: true });
    buscarRankingClube(clubeId, ranking.dados.proximoCursor, controller.signal)
      .then((pagina) => {
        if (controller.signal.aborted) return;
        setRanking((atual) => {
          if (atual.situacao !== 'sucesso') return atual;
          const jaListados = new Set(
            atual.dados.ranking.map((linha) => linha.usuario.id)
          );
          return {
            situacao: 'sucesso',
            carregandoMais: false,
            dados: {
              ...pagina,
              ranking: [
                ...atual.dados.ranking,
                ...pagina.ranking.filter(
                  (linha) => !jaListados.has(linha.usuario.id)
                ),
              ],
            },
          };
        });
      })
      .catch(() => {
        if (controller.signal.aborted) return;
        setRanking((atual) =>
          atual.situacao === 'sucesso'
            ? { ...atual, carregandoMais: false }
            : atual
        );
      });
  }, [clubeId, ranking]);

  useFocusEffect(
    useCallback(() => {
      recarregar();
      return () => {
        requisicao.current?.abort();
        requisicaoMais.current?.abort();
      };
    }, [recarregar])
  );

  return { ranking, recarregar, carregarMais };
}
