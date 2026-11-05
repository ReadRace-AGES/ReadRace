import { useCallback, useEffect, useRef, useState } from 'react';

import { mensagemErroCarregamento } from '@/api/client';

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
  /** Atualiza ao voltar à tela, preservando conteúdo e profundidade das listas. */
  atualizar: () => void;
  /** Anexa a página seguinte de uma lista. Ignora se não há mais ou se já está buscando. */
  carregarMais: (lista: ListaBiblioteca) => void;
};

// Só a mensagem do envelope da API é exibível (#10); erro de rede vira o texto padrão.
function mensagemDe(erro: unknown) {
  return mensagemErroCarregamento(erro);
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
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });
  const estadoAtual = useRef(biblioteca);
  estadoAtual.current = biblioteca;
  const atualizando = useRef(false);
  // Cancela as páginas em voo quando a tela recarrega ou desmonta.
  const controllers = useRef(new Set<AbortController>());

  useEffect(() => {
    const controller = new AbortController();
    const anterior = estadoAtual.current;
    atualizando.current = true;
    if (!tentativa.silenciosa || anterior.situacao !== 'sucesso') {
      setBiblioteca({ situacao: 'carregando' });
    } else {
      // Uma paginação interrompida não deve deixar "Ver mais" travado.
      setBiblioteca({
        situacao: 'sucesso',
        dados: Object.fromEntries(
          LISTAS_BIBLIOTECA.map((lista) => [
            lista,
            { ...anterior.dados[lista], carregandoMais: false },
          ])
        ) as Biblioteca,
      });
    }
    async function buscarAtualizada() {
      const dados = primeiraPagina(await buscarBiblioteca(controller.signal));
      if (tentativa.silenciosa && anterior.situacao === 'sucesso') {
        // Os cursores antigos podem mudar após uma leitura. Recomeça do servidor,
        // mas mantém a quantidade já explorada em "Ver mais".
        await Promise.all(
          LISTAS_BIBLIOTECA.map(async (lista) => {
            const nova = dados[lista];
            while (
              !controller.signal.aborted &&
              nova.proximoCursor &&
              nova.itens.length < anterior.dados[lista].itens.length
            ) {
              const pagina = await buscarPaginaBiblioteca(
                lista,
                nova.proximoCursor,
                controller.signal
              );
              nova.itens.push(...pagina.itens);
              nova.proximoCursor = pagina.proximoCursor;
            }
          })
        );
      }
      return dados;
    }
    buscarAtualizada()
      .then((dados) => {
        if (controller.signal.aborted) return;
        setBiblioteca({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setBiblioteca((atual) =>
          tentativa.silenciosa && atual.situacao === 'sucesso'
            ? atual
            : { situacao: 'erro', mensagem: mensagemDe(erro) }
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) atualizando.current = false;
      });
    return () => {
      controller.abort();
      controllers.current.forEach((c) => c.abort());
      controllers.current.clear();
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
      if (atualizando.current || biblioteca.situacao !== 'sucesso') return;
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

  return { biblioteca, recarregar, atualizar, carregarMais };
}
