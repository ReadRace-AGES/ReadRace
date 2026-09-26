import { useCallback, useEffect, useRef, useState } from 'react';

import { ApiError } from '@/api/client';
import { curtir, descurtir } from '@/features/posts/api';

import { buscarForumDoClube, type ForumClube, type PostForum } from './api';

export type ForumCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: ForumClube };

export type ForumClubeState = {
  forum: ForumCarga;
  recarregar: () => void;
  alternarCurtida: (postId: string) => Promise<void>;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o fórum do clube.';
}

function mensagemDeCurtida(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível registrar sua curtida. Tente novamente.';
}

function comPost(
  dados: ForumClube,
  postId: string,
  transformar: (post: PostForum) => PostForum
): ForumClube {
  return {
    ...dados,
    posts: dados.posts.map((post) =>
      post.id === postId ? transformar(post) : post
    ),
  };
}

/** Cabeçalho e posts chegam na mesma resposta: nenhum card sai meio renderizado. */
export function useForumClube(clubeId: string): ForumClubeState {
  const [forum, setForum] = useState<ForumCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);
  // Espelha `forum` de forma síncrona: o updater de setState roda no momento em que
  // o React decide, não no momento da chamada — ler o post atual precisa de algo síncrono.
  const forumRef = useRef(forum);
  forumRef.current = forum;

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

  const alternarCurtida = useCallback(async (postId: string) => {
    const atual = forumRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados.posts.find((post) => post.id === postId);
    if (!anterior) return;

    setForum({
      situacao: 'sucesso',
      dados: comPost(atual.dados, postId, (post) => ({
        ...post,
        curtidoPorMim: !post.curtidoPorMim,
        totalCurtidas: post.totalCurtidas + (post.curtidoPorMim ? -1 : 1),
      })),
    });

    try {
      await (anterior.curtidoPorMim ? descurtir : curtir)(postId);
    } catch (erro) {
      setForum((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: comPost(maisRecente.dados, postId, () => anterior),
            }
          : maisRecente
      );
      throw new Error(mensagemDeCurtida(erro));
    }
  }, []);

  return { forum, recarregar, alternarCurtida };
}
