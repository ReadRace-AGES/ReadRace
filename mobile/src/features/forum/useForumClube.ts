import { useCallback, useEffect, useRef, useState } from 'react';

import { ApiError, mensagemErroCarregamento } from '@/api/client';
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
  curtidasPendentes: Set<string>;
  adicionarPost: (post: PostForum) => void;
  registrarComentario: (postId: string) => void;
};

type SessaoForum = {
  clubeId: string;
  controller: AbortController;
  revisao: number;
  curtidas: Set<string>;
};

function mensagemDe(erro: unknown) {
  return mensagemErroCarregamento(erro);
}

function mensagemDeCurtida(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'NÃ£o foi possÃ­vel registrar sua curtida. Tente novamente.';
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

/** CabeÃ§alho e posts chegam na mesma resposta: nenhum card sai meio renderizado. */
export function useForumClube(clubeId: string): ForumClubeState {
  const [forum, setForum] = useState<ForumCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);
  const [curtidasPendentes, setCurtidasPendentes] = useState<Set<string>>(
    new Set()
  );
  const sessaoRef = useRef<SessaoForum | null>(null);
  // Espelha `forum` de forma sÃ­ncrona: o updater de setState roda no momento em que
  // o React decide, nÃ£o no momento da chamada â€” ler o post atual precisa de algo sÃ­ncrono.
  const forumRef = useRef(forum);
  forumRef.current = forum;

  useEffect(() => {
    const sessao: SessaoForum = {
      clubeId,
      controller: new AbortController(),
      revisao: 0,
      curtidas: new Set(),
    };
    sessaoRef.current = sessao;
    setForum({ situacao: 'carregando' });
    setCurtidasPendentes(new Set());
    return () => sessao.controller.abort();
  }, [clubeId]);

  useEffect(() => {
    const sessao = sessaoRef.current!;
    const revisao = sessao.revisao;
    const haviaCurtidasPendentes = sessao.curtidas.size > 0;
    const controller = new AbortController();
    const cancelado = () =>
      controller.signal.aborted || sessao.controller.signal.aborted;
    // Uma recarga mantÃ©m o conteÃºdo visÃ­vel. Se houver mutaÃ§Ã£o enquanto o GET
    // estiver pendente, a resposta antiga nÃ£o substitui posts ou curtidas locais.
    setForum((atual) =>
      atual.situacao === 'sucesso' ? atual : { situacao: 'carregando' }
    );
    buscarForumDoClube(clubeId, controller.signal)
      .then((dados) => {
        if (cancelado()) return;
        setForum((atual) =>
          atual.situacao === 'sucesso' &&
          (sessao.revisao !== revisao || haviaCurtidasPendentes)
            ? atual
            : { situacao: 'sucesso', dados }
        );
      })
      .catch((erro: unknown) => {
        if (cancelado()) return;
        setForum((atual) =>
          atual.situacao === 'sucesso'
            ? atual
            : { situacao: 'erro', mensagem: mensagemDe(erro) }
        );
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  const adicionarPost = useCallback(
    (post: PostForum) => {
      const sessao = sessaoRef.current;
      if (
        !sessao ||
        sessao.clubeId !== clubeId ||
        sessao.controller.signal.aborted
      )
        return;
      sessao.revisao++;
      setForum((atual) =>
        atual.situacao === 'sucesso'
          ? {
              ...atual,
              dados: {
                ...atual.dados,
                posts: [
                  post,
                  ...atual.dados.posts.filter((item) => item.id !== post.id),
                ],
              },
            }
          : atual
      );
    },
    [clubeId]
  );

  const registrarComentario = useCallback(
    (postId: string) => {
      const sessao = sessaoRef.current;

      if (
        !sessao ||
        sessao.clubeId !== clubeId ||
        sessao.controller.signal.aborted
      ) {
        return;
      }

      sessao.revisao++;

      setForum((atual) =>
        atual.situacao === 'sucesso'
          ? {
              ...atual,
              dados: comPost(atual.dados, postId, (post) => ({
                ...post,
                totalComentarios: (post.totalComentarios ?? 0) + 1,
              })),
            }
          : atual
      );
    },
    [clubeId]
  );
  const alternarCurtida = useCallback(
    async (postId: string) => {
      const sessao = sessaoRef.current;
      if (
        !sessao ||
        sessao.clubeId !== clubeId ||
        sessao.controller.signal.aborted ||
        sessao.curtidas.has(postId)
      )
        return;
      const atual = forumRef.current;
      if (atual.situacao !== 'sucesso') return;
      const anterior = atual.dados.posts.find((post) => post.id === postId);
      if (!anterior) return;

      sessao.curtidas.add(postId);
      sessao.revisao++;
      setCurtidasPendentes(new Set(sessao.curtidas));
      setForum((maisRecente) =>
        maisRecente.situacao === 'sucesso'
          ? {
              ...maisRecente,
              dados: comPost(maisRecente.dados, postId, (post) => ({
                ...post,
                curtidoPorMim: !anterior.curtidoPorMim,
                totalCurtidas:
                  anterior.totalCurtidas + (anterior.curtidoPorMim ? -1 : 1),
              })),
            }
          : maisRecente
      );

      try {
        await (anterior.curtidoPorMim ? descurtir : curtir)(
          postId,
          sessao.controller.signal
        );
      } catch (erro) {
        if (sessao.controller.signal.aborted) return;
        sessao.revisao++;
        setForum((maisRecente) =>
          maisRecente.situacao === 'sucesso'
            ? {
                situacao: 'sucesso',
                dados: comPost(maisRecente.dados, postId, () => anterior),
              }
            : maisRecente
        );
        throw new Error(mensagemDeCurtida(erro));
      } finally {
        if (!sessao.controller.signal.aborted) {
          sessao.curtidas.delete(postId);
          setCurtidasPendentes(new Set(sessao.curtidas));
        }
      }
    },
    [clubeId]
  );

  return {
    forum,
    recarregar,
    alternarCurtida,
    curtidasPendentes,
    adicionarPost,
    registrarComentario,
  };
}
