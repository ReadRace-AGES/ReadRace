import { useCallback, useEffect, useRef, useState } from 'react';

import { mensagemErroCarregamento } from '@/api/client';
import { curtir, descurtir } from '@/features/posts/api';

import {
  buscarComunidade,
  deixarDeSeguirComunidade,
  seguirComunidade,
  type Comunidade,
  type OrdemComunidade,
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

type SobreposicaoCurtida = {
  token: object;
  curtidoPorMim: boolean;
  totalCurtidas: number;
  confirmadaEm: number | null;
};

type SobreposicaoSeguimento = {
  token: object;
  sigo: boolean;
  seguidores: number;
  confirmadaEm: number | null;
};

function aplicarSobreposicoes(
  dados: Comunidade,
  curtidas: Map<string, SobreposicaoCurtida>,
  seguimento: SobreposicaoSeguimento | null
) {
  return {
    ...dados,
    ...(seguimento
      ? { sigo: seguimento.sigo, seguidores: seguimento.seguidores }
      : {}),
    posts: dados.posts.map((post) => {
      const sobreposicao = curtidas.get(post.id);
      return sobreposicao
        ? {
            ...post,
            curtidoPorMim: sobreposicao.curtidoPorMim,
            totalCurtidas: sobreposicao.totalCurtidas,
          }
        : post;
    }),
  };
}

export function useComunidade(
  comunidadeId: string,
  ordem: OrdemComunidade = 'recentes'
) {
  const [comunidade, setComunidade] = useState<ComunidadeCarga>({
    situacao: 'carregando',
  });
  const [carregandoPosts, setCarregandoPosts] = useState(true);
  const [erroPosts, setErroPosts] = useState<string | null>(null);
  const [versao, setVersao] = useState(0);
  const comunidadeRef = useRef(comunidade);
  const requisicaoAtualRef = useRef(0);
  const revisaoMutacoesRef = useRef(0);
  const curtidasRef = useRef(new Map<string, SobreposicaoCurtida>());
  const seguimentoRef = useRef<SobreposicaoSeguimento | null>(null);
  useEffect(() => {
    comunidadeRef.current = comunidade;
  }, [comunidade]);

  useEffect(() => {
    const controller = new AbortController();
    const requisicao = ++requisicaoAtualRef.current;
    const revisaoNoInicio = revisaoMutacoesRef.current;
    const haviaDados = comunidadeRef.current.situacao === 'sucesso';
    setCarregandoPosts(true);
    setErroPosts(null);
    buscarComunidade(comunidadeId, ordem, controller.signal)
      .then((dados) => {
        if (
          !controller.signal.aborted &&
          requisicao === requisicaoAtualRef.current
        ) {
          const seguimento = seguimentoRef.current;
          const dadosComAlteracoes = aplicarSobreposicoes(
            dados,
            curtidasRef.current,
            seguimento
          );
          setComunidade({ situacao: 'sucesso', dados: dadosComAlteracoes });
          for (const [postId, sobreposicao] of curtidasRef.current) {
            if (
              sobreposicao.confirmadaEm !== null &&
              revisaoNoInicio >= sobreposicao.confirmadaEm
            ) {
              curtidasRef.current.delete(postId);
            }
          }
          if (
            seguimento !== null &&
            seguimento.confirmadaEm !== null &&
            revisaoNoInicio >= seguimento.confirmadaEm
          ) {
            seguimentoRef.current = null;
          }
          setCarregandoPosts(false);
        }
      })
      .catch((erro: unknown) => {
        if (
          !controller.signal.aborted &&
          requisicao === requisicaoAtualRef.current
        ) {
          setCarregandoPosts(false);
          if (haviaDados) {
            setErroPosts(mensagemDe(erro));
          } else {
            setComunidade({ situacao: 'erro', mensagem: mensagemDe(erro) });
          }
        }
      });
    return () => controller.abort();
  }, [comunidadeId, ordem, versao]);

  const recarregar = useCallback(() => {
    if (comunidadeRef.current.situacao !== 'sucesso') {
      setComunidade({ situacao: 'carregando' });
    }
    setVersao((valor) => valor + 1);
  }, []);

  const alternarSeguimento = useCallback(async () => {
    const atual = comunidadeRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados;
    const token = {};
    seguimentoRef.current = {
      token,
      sigo: !anterior.sigo,
      seguidores: anterior.seguidores + (anterior.sigo ? -1 : 1),
      confirmadaEm: null,
    };
    revisaoMutacoesRef.current++;
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
      if (seguimentoRef.current?.token === token) {
        const confirmadaEm = ++revisaoMutacoesRef.current;
        seguimentoRef.current = {
          token,
          sigo: resposta.sigo,
          seguidores: resposta.seguidores,
          confirmadaEm,
        };
      }
      if (seguimentoRef.current?.token === token) {
        setComunidade((maisRecente) =>
          maisRecente.situacao === 'sucesso'
            ? {
                situacao: 'sucesso',
                dados: { ...maisRecente.dados, ...resposta },
              }
            : maisRecente
        );
      }
    } catch (erro) {
      if (seguimentoRef.current?.token === token) {
        seguimentoRef.current = null;
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
      }
      throw erro;
    }
  }, [comunidadeId]);

  const alternarCurtida = useCallback(async (postId: string) => {
    const atual = comunidadeRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados.posts.find((post) => post.id === postId);
    if (!anterior) return;
    const token = {};
    curtidasRef.current.set(postId, {
      token,
      curtidoPorMim: !anterior.curtidoPorMim,
      totalCurtidas:
        anterior.totalCurtidas + (anterior.curtidoPorMim ? -1 : 1),
      confirmadaEm: null,
    });
    revisaoMutacoesRef.current++;
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
      if (curtidasRef.current.get(postId)?.token === token) {
        const confirmadaEm = ++revisaoMutacoesRef.current;
        curtidasRef.current.set(postId, {
          token,
          totalCurtidas: resposta.totalCurtidas,
          curtidoPorMim: resposta.curtidoPorMim,
          confirmadaEm,
        });
      }
      if (curtidasRef.current.get(postId)?.token === token) {
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
      }
    } catch (erro) {
      if (curtidasRef.current.get(postId)?.token === token) {
        curtidasRef.current.delete(postId);
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
      }
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

  const adicionarPost = useCallback(
    (post: PostComunidade) => {
      setComunidade((atual) =>
        atual.situacao === 'sucesso' && atual.dados.id === comunidadeId
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
    [comunidadeId]
  );

  return {
    comunidade,
    carregandoPosts,
    erroPosts,
    recarregar,
    alternarSeguimento,
    alternarCurtida,
    somarComentario,
    adicionarPost,
  };
}
