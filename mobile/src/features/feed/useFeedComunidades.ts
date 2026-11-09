import { useCallback, useEffect, useRef, useState } from 'react';

import { mensagemErroCarregamento } from '@/api/client';
import { seguirComunidade } from '@/features/comunidade/api';

import {
  buscarFeedComunidades,
  type ComunidadeFeed,
  type ComunidadeSugeridaFeed,
  type FeedComunidades,
} from './api';

/** As listas chegam juntas, sem seções parcialmente carregadas. */
export type FeedCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: FeedComunidades };

export type FeedComunidadesState = {
  feed: FeedCarga;
  recarregar: () => void;
  atualizar: () => void;
  participacoesPendentes: Set<string>;
  participar: (comunidade: ComunidadeSugeridaFeed) => Promise<boolean>;
};

function mensagemDe(erro: unknown) {
  return mensagemErroCarregamento(erro);
}

function incluirParticipacao(
  dados: FeedComunidades,
  comunidade: ComunidadeFeed
): FeedComunidades {
  return {
    ...dados,
    comunidades: [
      ...dados.comunidades.filter((item) => item.id !== comunidade.id),
      comunidade,
    ],
    comunidadesSugeridas: (dados.comunidadesSugeridas ?? []).filter(
      (item) => item.id !== comunidade.id
    ),
  };
}

export function useFeedComunidades(): FeedComunidadesState {
  const [feed, setFeed] = useState<FeedCarga>({ situacao: 'carregando' });
  const pendentes = useRef(new Set<string>());
  const [participacoesPendentes, setParticipacoesPendentes] = useState(
    new Set<string>()
  );
  const versao = useRef(0);
  const concluidas = useRef(
    new Map<string, { versao: number; comunidade: ComunidadeFeed }>()
  );
  // Silenciosa é a busca de fundo ao voltar para a aba: quem já está vendo o feed não perde o
  // conteúdo para o carregando, nem para um erro passageiro.
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });

  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const versaoInicio = versao.current;
    const aplicar = (estado: FeedCarga, preservaConteudo: boolean) => {
      if (!ativo) return;
      setFeed((atual) =>
        preservaConteudo && atual.situacao === 'sucesso' ? atual : estado
      );
    };
    aplicar({ situacao: 'carregando' }, tentativa.silenciosa);
    buscarFeedComunidades(controller.signal)
      .then((dados) => {
        // Só sobrepõe sucessos posteriores ao início deste GET. Um GET novo
        // permanece autoritativo para seguir/deixar de seguir pelo perfil.
        for (const entrada of concluidas.current.values()) {
          if (entrada.versao > versaoInicio)
            dados = incluirParticipacao(dados, entrada.comunidade);
        }
        aplicar({ situacao: 'sucesso', dados }, false);
      })
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
    () =>
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: false })),
    []
  );
  const atualizar = useCallback(
    () =>
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: true })),
    []
  );

  const participar = useCallback(async (sugestao: ComunidadeSugeridaFeed) => {
    if (pendentes.current.has(sugestao.id)) return false;
    pendentes.current.add(sugestao.id);
    setParticipacoesPendentes(new Set(pendentes.current));
    try {
      const resposta = await seguirComunidade(sugestao.id);
      if (!resposta.sigo) throw new Error('Participação não confirmada');
      const comunidade: ComunidadeFeed = {
        id: sugestao.id,
        nome: sugestao.nome,
        capaUrl: sugestao.imagemUrl,
        totalMembros: resposta.seguidores,
      };
      concluidas.current.set(comunidade.id, {
        versao: ++versao.current,
        comunidade,
      });
      setFeed((atual) =>
        atual.situacao === 'sucesso'
          ? { ...atual, dados: incluirParticipacao(atual.dados, comunidade) }
          : atual
      );
      return true;
    } finally {
      pendentes.current.delete(sugestao.id);
      setParticipacoesPendentes(new Set(pendentes.current));
    }
  }, []);

  return { feed, recarregar, atualizar, participacoesPendentes, participar };
}
