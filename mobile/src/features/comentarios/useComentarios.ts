import { useCallback, useEffect, useRef, useState } from 'react';

import { ApiError } from '@/api/client';
import { curtir, descurtir } from '@/features/posts/api';

import {
  comentarPost,
  listarComentarios,
  type Comentario,
  type ComentariosDoPost,
} from './api';
import { buscarComentario, comComentario, inserirNaThread } from './model';

export type ComentariosCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: ComentariosDoPost };

type Estado = { chave: string | null; carga: ComentariosCarga };

const CARREGANDO: ComentariosCarga = { situacao: 'carregando' };

/** Troca a lista da carga `chave`; outra carga (de outra abertura) fica como está. */
function naThread(
  chave: string,
  transformar: (lista: Comentario[]) => Comentario[]
) {
  return (atual: Estado): Estado =>
    atual.chave === chave && atual.carga.situacao === 'sucesso'
      ? {
          chave,
          carga: {
            situacao: 'sucesso',
            dados: {
              ...atual.carga.dados,
              comentarios: transformar(atual.carga.dados.comentarios),
            },
          },
        }
      : atual;
}

/**
 * Thread de comentários de um post (#150). Com `postId` nulo (painel fechado), nada
 * é buscado e o último estado fica, para o painel não esvaziar enquanto desce. Cada
 * abertura busca de novo.
 *
 * A carga guarda de qual busca veio (`chave`): enquanto a busca atual não responde,
 * o hook devolve "carregando", sem `setState` dentro do efeito.
 */
export function useComentarios(postId: string | null) {
  const [versao, setVersao] = useState(0);
  const [estado, setEstado] = useState<Estado>({
    chave: null,
    carga: CARREGANDO,
  });
  const estadoRef = useRef(estado);
  useEffect(() => {
    estadoRef.current = estado;
  }, [estado]);
  const [curtidasPendentes, setCurtidasPendentes] = useState<Set<string>>(
    () => new Set()
  );
  const chave = postId ? `${postId}#${versao}` : null;

  useEffect(() => {
    if (!postId || !chave) return;
    const controller = new AbortController();
    listarComentarios(postId, controller.signal)
      .then((dados) => {
        if (!controller.signal.aborted) {
          setEstado({ chave, carga: { situacao: 'sucesso', dados } });
        }
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setEstado({
          chave,
          carga: {
            situacao: 'erro',
            mensagem:
              erro instanceof ApiError
                ? erro.message
                : 'Não foi possível carregar os comentários.',
          },
        });
      });
    return () => controller.abort();
  }, [postId, chave]);

  const recarregar = useCallback(() => setVersao((valor) => valor + 1), []);

  /**
   * Comenta o post ou, com `respondendoA`, responde àquele comentário. Rejeita com o
   * erro original; quem chama decide o aviso e mantém o texto.
   */
  const comentar = useCallback(
    async (
      texto: string,
      respondendoA: string | null,
      signal?: AbortSignal
    ): Promise<Comentario> => {
      if (!postId || !chave) throw new Error('Nenhum post aberto.');
      const novo = await comentarPost(respondendoA ?? postId, texto, signal);
      setEstado(
        naThread(chave, (lista) =>
          buscarComentario(lista, novo.id)
            ? lista
            : inserirNaThread(lista, respondendoA, novo)
        )
      );
      return novo;
    },
    [postId, chave]
  );

  /** Curtida otimista do comentário: volta ao que era se a API falhar. */
  const alternarCurtida = useCallback(
    async (comentarioId: string) => {
      const atual = estadoRef.current;
      if (!chave || atual.chave !== chave || atual.carga.situacao !== 'sucesso')
        return;
      const anterior = buscarComentario(
        atual.carga.dados.comentarios,
        comentarioId
      );
      if (!anterior) return;
      const marcar = (pendente: boolean) =>
        setCurtidasPendentes((ids) => {
          const proximo = new Set(ids);
          if (pendente) proximo.add(comentarioId);
          else proximo.delete(comentarioId);
          return proximo;
        });
      const aplicar = (curtidoPorMim: boolean, totalCurtidas: number) =>
        setEstado(
          naThread(chave, (lista) =>
            comComentario(lista, comentarioId, (comentario) => ({
              ...comentario,
              curtidoPorMim,
              totalCurtidas,
            }))
          )
        );

      marcar(true);
      aplicar(
        !anterior.curtidoPorMim,
        anterior.totalCurtidas + (anterior.curtidoPorMim ? -1 : 1)
      );
      try {
        const resposta = await (anterior.curtidoPorMim ? descurtir : curtir)(
          comentarioId
        );
        aplicar(resposta.curtidoPorMim, resposta.totalCurtidas);
      } catch (erro) {
        aplicar(anterior.curtidoPorMim, anterior.totalCurtidas);
        throw erro;
      } finally {
        marcar(false);
      }
    },
    [chave]
  );

  const carga =
    chave === null || estado.chave === chave ? estado.carga : CARREGANDO;

  return { carga, recarregar, comentar, alternarCurtida, curtidasPendentes };
}
