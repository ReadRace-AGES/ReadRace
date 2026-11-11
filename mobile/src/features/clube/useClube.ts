import { useCallback, useEffect, useRef, useState } from 'react';

import { mensagemErroCarregamento } from '@/api/client';

import { buscarClube, type Clube } from './api';

export type ClubeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Clube };

export type ClubeState = {
  clube: ClubeCarga;
  recarregar: () => void;
  atualizar: () => void;
  pedidoEnviado: () => void;
  cancelarCarregamento: () => void;
};

function mensagemDe(erro: unknown) {
  return mensagemErroCarregamento(erro);
}

/**
 * Cabeçalho e ranking chegam na mesma resposta, então a tela nunca mostra meia página.
 *
 * Recarregar depois de registrar leitura é o comportamento pedido pela #35. Voltar do quiz
 * (#162) também recarrega: cada acerto soma `Pontos` e muda o ranking.
 */
export function useClube(clubeId: string): ClubeState {
  const [resultado, setResultado] = useState<{ id: string; carga: ClubeCarga }>(
    { id: clubeId, carga: { situacao: 'carregando' } }
  );
  const clube: ClubeCarga =
    resultado.id === clubeId ? resultado.carga : { situacao: 'carregando' };
  const setClube = useCallback(
    (aplicar: ClubeCarga | ((atual: ClubeCarga) => ClubeCarga)) => {
      setResultado((atual) => ({
        id: clubeId,
        carga:
          typeof aplicar === 'function'
            ? aplicar(
                atual.id === clubeId ? atual.carga : { situacao: 'carregando' }
              )
            : aplicar,
      }));
    },
    [clubeId]
  );
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });
  const requisicao = useRef<AbortController | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    requisicao.current = controller;
    setClube((atual) =>
      tentativa.silenciosa &&
      atual.situacao === 'sucesso' &&
      atual.dados.id === clubeId
        ? atual
        : { situacao: 'carregando' }
    );
    buscarClube(clubeId, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setClube({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setClube((atual) =>
          tentativa.silenciosa && atual.situacao === 'sucesso'
            ? atual
            : { situacao: 'erro', mensagem: mensagemDe(erro) }
        );
      });
    return () => controller.abort();
  }, [clubeId, tentativa, setClube]);

  const cancelarCarregamento = useCallback(
    () => requisicao.current?.abort(),
    []
  );

  const consultar = useCallback((silenciosa: boolean) => {
    requisicao.current?.abort();
    setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa }));
  }, []);
  const recarregar = useCallback(() => consultar(false), [consultar]);
  const atualizar = useCallback(() => consultar(true), [consultar]);

  const pedidoEnviado = useCallback(() => {
    requisicao.current?.abort();
    setClube((atual) =>
      atual.situacao === 'sucesso' && atual.dados.id === clubeId
        ? { ...atual, dados: { ...atual.dados, situacao: 'pendente' } }
        : atual
    );
  }, [clubeId, setClube]);

  return {
    cancelarCarregamento,
    pedidoEnviado,
    clube,
    recarregar,
    atualizar,
  };
}
