import { useCallback, useEffect, useRef, useState } from 'react';

import { ApiError } from '@/api/client';

import {
  buscarBiblioteca,
  buscarPaginaBiblioteca,
  LISTAS_BIBLIOTECA,
  type BibliotecaResponse,
  type ListaBiblioteca,
  type LivroBiblioteca,
} from './api';

/** Uma lista da tela com o que já foi carregado e o que falta. */
export type ListaCarregada = {
  itens: LivroBiblioteca[];
  /** Nulo quando a lista acabou. */
  proximoCursor: string | null;
  carregandoMais: boolean;
  /** A última página falhou; o cursor foi mantido para tentar de novo. */
  erroAoCarregarMais: boolean;
};

export type Biblioteca = Record<ListaBiblioteca, ListaCarregada>;

/** Estado da chamada inicial: a tela nunca mostra dado parcial nem fica presa carregando. */
export type Carga<T> =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: T };

export type BibliotecaState = {
  biblioteca: Carga<Biblioteca>;
  /** Refaz a chamada inicial; é a ação de "tentar de novo". */
  recarregar: () => void;
  /** Anexa a página seguinte de uma lista. Ignora se não há mais ou se já está buscando. */
  carregarMais: (lista: ListaBiblioteca) => void;
};

// Só a mensagem do envelope da API é exibível (#10); erro de rede vira o texto padrão.
function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar sua biblioteca.';
}

function primeiraPagina(resposta: BibliotecaResponse): Biblioteca {
  const listas = {} as Biblioteca;
  for (const lista of LISTAS_BIBLIOTECA) {
    listas[lista] = {
      itens: resposta[lista].itens,
      proximoCursor: resposta[lista].proximoCursor,
      carregandoMais: false,
      erroAoCarregarMais: false,
    };
  }
  return listas;
}

/** Carrega a biblioteca do usuário atual e pagina cada lista por cursor. */
export function useBiblioteca(): BibliotecaState {
  const [biblioteca, setBiblioteca] = useState<Carga<Biblioteca>>({
    situacao: 'carregando',
  });
  const [versao, setVersao] = useState(0);
  // Cancela as páginas em voo quando a tela recarrega ou desmonta.
  const controllers = useRef(new Set<AbortController>());

  // Carrega uma vez por montagem (e a cada "tentar de novo"), nao a cada foco: refazer
  // a chamada ao voltar do detalhe descartaria as paginas ja trazidas pelo "Ver mais" e
  // devolveria a tela ao estado de carregando. Meus Livros nao exibe progresso, entao
  // nao ha o que atualizar no retorno.
  useEffect(() => {
    const controller = new AbortController();
    setBiblioteca({ situacao: 'carregando' });
    buscarBiblioteca(controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setBiblioteca({ situacao: 'sucesso', dados: primeiraPagina(dados) });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setBiblioteca({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => {
      controller.abort();
      controllers.current.forEach((c) => c.abort());
      controllers.current.clear();
    };
  }, [versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  const atualizarLista = useCallback(
    (
      lista: ListaBiblioteca,
      mudanca: (atual: ListaCarregada) => ListaCarregada
    ) => {
      setBiblioteca((estado) =>
        estado.situacao === 'sucesso'
          ? {
              situacao: 'sucesso',
              dados: { ...estado.dados, [lista]: mudanca(estado.dados[lista]) },
            }
          : estado
      );
    },
    []
  );

  const carregarMais = useCallback(
    (lista: ListaBiblioteca) => {
      if (biblioteca.situacao !== 'sucesso') return;
      const atual = biblioteca.dados[lista];
      if (!atual.proximoCursor || atual.carregandoMais) return;

      const controller = new AbortController();
      controllers.current.add(controller);
      atualizarLista(lista, (l) => ({
        ...l,
        carregandoMais: true,
        erroAoCarregarMais: false,
      }));

      buscarPaginaBiblioteca(lista, atual.proximoCursor, controller.signal)
        .then((pagina) => {
          if (controller.signal.aborted) return;
          atualizarLista(lista, (l) => ({
            itens: [...l.itens, ...pagina.itens],
            proximoCursor: pagina.proximoCursor,
            carregandoMais: false,
            erroAoCarregarMais: false,
          }));
        })
        .catch(() => {
          if (controller.signal.aborted) return;
          atualizarLista(lista, (l) => ({
            ...l,
            carregandoMais: false,
            erroAoCarregarMais: true,
          }));
        })
        .finally(() => controllers.current.delete(controller));
    },
    [biblioteca, atualizarLista]
  );

  return { biblioteca, recarregar, carregarMais };
}
