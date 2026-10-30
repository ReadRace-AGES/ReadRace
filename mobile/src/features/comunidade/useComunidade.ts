import { useCallback, useEffect, useRef, useState } from 'react';

import { mensagemErroCarregamento } from '@/api/client';
import { curtir, descurtir } from '@/features/posts/api';

import {
  buscarComunidade,
  deixarDeSeguirComunidade,
  seguirComunidade,
  type Comunidade,
  type PostComunidade,
} from './api';

export type ComunidadeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Comunidade };

function mensagemDe(erro: unknown) {
  return mensagemErroCarregamento(erro);
}

function comPost(
  dados: Comunidade,
  postId: string,
  transformar: (post: PostComunidade) => PostComunidade
) {
  return {
    ...dados,
    posts: dados.posts.map((post) =>
      post.id === postId ? transformar(post) : post
    ),
  };
}

export function useComunidade(comunidadeId: string) {
  const [comunidade, setComunidade] = useState<ComunidadeCarga>({
    situacao: 'carregando',
  });
  const [versao, setVersao] = useState(0);
  const comunidadeRef = useRef(comunidade);
  useEffect(() => {
    comunidadeRef.current = comunidade;
  }, [comunidade]);

  useEffect(() => {
    const controller = new AbortController();
    buscarComunidade(comunidadeId, controller.signal)
      .then((dados) => {
        if (!controller.signal.aborted)
          setComunidade({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (!controller.signal.aborted) {
          setComunidade({ situacao: 'erro', mensagem: mensagemDe(erro) });
        }
      });
    return () => controller.abort();
  }, [comunidadeId, versao]);

  const recarregar = useCallback(() => {
    setComunidade({ situacao: 'carregando' });
    setVersao((valor) => valor + 1);
  }, []);

  const alternarSeguimento = useCallback(async () => {
    const atual = comunidadeRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados;
    setComunidade((maisRecente) =>
      maisRecente.situacao === 'sucesso'
        ? {
            situacao: 'sucesso',
            dados: {
              ...maisRecente.dados,
              sigo: !anterior.sigo,
              seguidores: anterior.seguidores + (anterior.sigo ? -1 : 1),
            },
          }
        : maisRecente
    );
    try {
      const resposta = anterior.sigo
        ? await deixarDeSeguirComunidade(comunidadeId)
        : await seguirComunidade(comunidadeId);
      setComunidade((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: { ...maisRecente.dados, ...resposta },
            }
          : maisRecente
      );
    } catch (erro) {
      setComunidade((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: {
                ...maisRecente.dados,
                sigo: anterior.sigo,
                seguidores: anterior.seguidores,
              },
            }
          : maisRecente
      );
      throw erro;
    }
  }, [comunidadeId]);

  const alternarCurtida = useCallback(async (postId: string) => {
    const atual = comunidadeRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados.posts.find((post) => post.id === postId);
    if (!anterior) return;
    setComunidade((maisRecente) =>
      maisRecente.situacao === 'sucesso'
        ? {
            situacao: 'sucesso',
            dados: comPost(maisRecente.dados, postId, (post) => ({
              ...post,
              curtidoPorMim: !post.curtidoPorMim,
              totalCurtidas: post.totalCurtidas + (post.curtidoPorMim ? -1 : 1),
            })),
          }
        : maisRecente
    );
    try {
      const resposta = await (anterior.curtidoPorMim ? descurtir : curtir)(
        postId
      );
      setComunidade((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: comPost(maisRecente.dados, postId, (post) => ({
                ...post,
                totalCurtidas: resposta.totalCurtidas,
                curtidoPorMim: resposta.curtidoPorMim,
              })),
            }
          : maisRecente
      );
    } catch (erro) {
      setComunidade((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              // Só a curtida volta: o resto do post pode ter mudado no meio tempo
              dados: comPost(maisRecente.dados, postId, (post) => ({
                ...post,
                curtidoPorMim: anterior.curtidoPorMim,
                totalCurtidas: anterior.totalCurtidas,
              })),
            }
          : maisRecente
      );
      throw erro;
    }
  }, []);

  /** Comentário criado no painel (#150): o balão sobe 1 sem recarregar a comunidade. */
  const somarComentario = useCallback((postId: string) => {
    setComunidade((maisRecente) =>
      maisRecente.situacao === 'sucesso'
        ? {
            situacao: 'sucesso',
            dados: comPost(maisRecente.dados, postId, (post) => ({
              ...post,
              totalComentarios: post.totalComentarios + 1,
            })),
          }
        : maisRecente
    );
  }, []);

  return {
    comunidade,
    recarregar,
    alternarSeguimento,
    alternarCurtida,
    somarComentario,
  };
}
