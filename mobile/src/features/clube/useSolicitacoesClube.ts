import { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { ApiError, mensagemErroCarregamento } from '@/api/client';
import {
  buscarSolicitacoes,
  responderSolicitacao,
  type RespostaSolicitacao,
  type SolicitacaoEntrada,
} from './api';

const ERRO_RESPOSTA = 'Não foi possível concluir. Tente novamente.';

type Carga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | {
      situacao: 'sucesso';
      solicitacoes: SolicitacaoEntrada[];
      erroAtualizacao?: string;
    };

type Estado = {
  clubeId: string;
  carga: Carga;
  acoes: Record<string, RespostaSolicitacao>;
};

type Feedback = {
  onFeedback: (mensagem: string) => void;
  onSemAcesso: (mensagem: string) => void;
};

export function useSolicitacoesClube(
  clubeId: string,
  { onFeedback, onSemAcesso }: Feedback
) {
  const [resultado, setResultado] = useState<Estado>({
    clubeId,
    carga: { situacao: 'carregando' },
    acoes: {},
  });
  const consulta = useRef<AbortController | null>(null);
  const respostas = useRef(new Map<string, AbortController>());
  const contexto = useRef<{ clubeId: string; ativo: boolean } | null>(null);
  const bloqueado = useRef(false);
  const aplicar = useCallback(
    (alterar: (atual: Estado) => Estado) => {
      setResultado((atual) =>
        alterar(
          atual.clubeId === clubeId
            ? atual
            : { clubeId, carga: { situacao: 'carregando' }, acoes: {} }
        )
      );
    },
    [clubeId]
  );

  const interromper = useCallback(() => {
    consulta.current?.abort();
    respostas.current.forEach((controller) => controller.abort());
    respostas.current.clear();
  }, []);

  const tratarIndisponibilidade = useCallback(
    (erro: unknown, fallback = ERRO_RESPOSTA) => {
      if (!(erro instanceof ApiError)) return false;
      if (
        (erro.status === 403 && erro.code === 'SO_LIDER_CRIA_QUIZ') ||
        (erro.status === 404 && erro.code === 'CLUBE_NAO_ENCONTRADO')
      ) {
        interromper();
        bloqueado.current = true;
        const mensagem = erro.publicMessage ?? fallback;
        aplicar((atual) => ({
          ...atual,
          carga: { situacao: 'erro', mensagem },
          acoes: {},
        }));
        // Voltar ao detalhe encerra a administração e consulta novamente o cargo atual pelo useClube.
        if (erro.status === 403) onSemAcesso(mensagem);
        return true;
      }
      return false;
    },
    [aplicar, interromper, onSemAcesso]
  );

  const consultar = useCallback(
    (silenciosa = false) => {
      if (!contexto.current?.ativo || contexto.current.clubeId !== clubeId)
        return;
      consulta.current?.abort();
      const controller = new AbortController();
      consulta.current = controller;
      bloqueado.current = false;
      aplicar((atual) => ({
        ...atual,
        carga:
          silenciosa && atual.carga.situacao === 'sucesso'
            ? { ...atual.carga, erroAtualizacao: undefined }
            : { situacao: 'carregando' },
      }));
      buscarSolicitacoes(clubeId, controller.signal)
        .then((dados) => {
          if (controller.signal.aborted) return;
          aplicar((atual) => ({
            ...atual,
            carga: { situacao: 'sucesso', solicitacoes: dados.solicitacoes },
          }));
        })
        .catch((erro: unknown) => {
          if (
            controller.signal.aborted ||
            tratarIndisponibilidade(erro, mensagemErroCarregamento(erro))
          )
            return;
          const mensagem = mensagemErroCarregamento(erro);
          aplicar((atual) => ({
            ...atual,
            carga:
              silenciosa && atual.carga.situacao === 'sucesso'
                ? { ...atual.carga, erroAtualizacao: mensagem }
                : { situacao: 'erro', mensagem },
          }));
        });
    },
    [clubeId, aplicar, tratarIndisponibilidade]
  );

  useFocusEffect(
    useCallback(() => {
      const atual = { clubeId, ativo: true };
      contexto.current = atual;
      aplicar((estado) => ({ ...estado, acoes: {} }));
      consultar(true);
      return () => {
        atual.ativo = false;
        interromper();
      };
    }, [clubeId, aplicar, consultar, interromper])
  );

  const carga: Carga =
    resultado.clubeId === clubeId
      ? resultado.carga
      : { situacao: 'carregando' };
  const responder = useCallback(
    async (id: string, resposta: RespostaSolicitacao) => {
      if (
        !contexto.current?.ativo ||
        contexto.current.clubeId !== clubeId ||
        bloqueado.current ||
        respostas.current.has(id) ||
        carga.situacao !== 'sucesso' ||
        !carga.solicitacoes.some((pedido) => pedido.id === id)
      )
        return;
      // Uma leitura anterior à resposta nunca pode recolocar o pedido removido.
      consulta.current?.abort();
      const controller = new AbortController();
      respostas.current.set(id, controller);
      aplicar((atual) => ({
        ...atual,
        acoes: { ...atual.acoes, [id]: resposta },
      }));
      try {
        await responderSolicitacao(clubeId, id, resposta, controller.signal);
        if (controller.signal.aborted) return;
        consulta.current?.abort();
        aplicar((atual) => ({
          ...atual,
          carga:
            atual.carga.situacao === 'sucesso'
              ? {
                  ...atual.carga,
                  solicitacoes: atual.carga.solicitacoes.filter(
                    (pedido) => pedido.id !== id
                  ),
                }
              : atual.carga,
        }));
        onFeedback(
          resposta === 'aceitar' ? 'Pedido aceito.' : 'Pedido recusado.'
        );
        consultar(true);
      } catch (erro: unknown) {
        if (controller.signal.aborted || tratarIndisponibilidade(erro)) return;
        onFeedback(
          erro instanceof ApiError
            ? (erro.publicMessage ?? ERRO_RESPOSTA)
            : ERRO_RESPOSTA
        );
        if (
          erro instanceof ApiError &&
          ((erro.status === 409 && erro.code === 'SOLICITACAO_JA_RESPONDIDA') ||
            (erro.status === 404 && erro.code === 'SOLICITACAO_NAO_ENCONTRADA'))
        )
          consultar(true);
      } finally {
        if (
          !controller.signal.aborted &&
          respostas.current.get(id) === controller
        ) {
          respostas.current.delete(id);
          aplicar((atual) => {
            const acoes = { ...atual.acoes };
            delete acoes[id];
            return { ...atual, acoes };
          });
        }
      }
    },
    [clubeId, carga, aplicar, consultar, onFeedback, tratarIndisponibilidade]
  );

  return {
    carga,
    acoes: resultado.clubeId === clubeId ? resultado.acoes : {},
    recarregar: consultar,
    responder,
  };
}
