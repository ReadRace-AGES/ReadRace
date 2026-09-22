import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/api/client';
import type { ProgressoLeituraResponse } from '@/features/progresso/api';
import { curtir, descurtir } from '@/features/posts/api';
import { buscarDetalhe, type LivroDetalhe, type PostLivro } from './api';
import { aplicarProgresso } from './model';

function mensagemDeCurtida(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível registrar sua curtida. Tente novamente.';
}

function comPost(
  dados: LivroDetalhe,
  postId: string,
  transformar: (post: PostLivro) => PostLivro
): LivroDetalhe {
  return {
    ...dados,
    posts: dados.posts.map((post) =>
      post.id === postId ? transformar(post) : post
    ),
  };
}

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: LivroDetalhe };

export function useLivroDetalhe(livroId: string) {
  const [estado, setEstado] = useState<Estado>({ situacao: 'carregando' });
  const [tentativa, setTentativa] = useState(0);
  // Espelha `estado` de forma síncrona: o updater de setState roda no momento em que
  // o React decide, não no momento da chamada — ler o post atual precisa de algo síncrono.
  const estadoRef = useRef(estado);
  estadoRef.current = estado;
  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const timeout = setTimeout(() => controller.abort(), 15000);
    setEstado({ situacao: 'carregando' });
    buscarDetalhe(livroId, controller.signal)
      .then((dados) => {
        if (ativo) setEstado({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (ativo)
          setEstado({
            situacao: 'erro',
            mensagem:
              erro instanceof ApiError
                ? erro.message
                : 'Não foi possível carregar o livro. Tente novamente.',
          });
      })
      .finally(() => clearTimeout(timeout));
    return () => {
      ativo = false;
      clearTimeout(timeout);
      controller.abort();
    };
  }, [livroId, tentativa]);
  const atualizarProgresso = useCallback(
    (resposta: ProgressoLeituraResponse) => {
      setEstado((atual) =>
        atual.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: aplicarProgresso(atual.dados, resposta),
            }
          : atual
      );
    },
    []
  );

  const alternarCurtida = useCallback(async (postId: string) => {
    const atual = estadoRef.current;
    if (atual.situacao !== 'sucesso') return;
    const anterior = atual.dados.posts.find((post) => post.id === postId);
    if (!anterior) return;

    setEstado({
      situacao: 'sucesso',
      dados: comPost(atual.dados, postId, (post) => ({
        ...post,
        curtidoPorMim: !post.curtidoPorMim,
        curtidas: post.curtidas + (post.curtidoPorMim ? -1 : 1),
      })),
    });

    try {
      await (anterior.curtidoPorMim ? descurtir : curtir)(postId);
    } catch (erro) {
      setEstado((maisRecente) =>
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

  return {
    estado,
    recarregar: () => setTentativa((t) => t + 1),
    atualizarProgresso,
    alternarCurtida,
  };
}
