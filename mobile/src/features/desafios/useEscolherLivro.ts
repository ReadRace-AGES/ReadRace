import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';
import {
  buscarBiblioteca,
  buscarPaginaBiblioteca,
  type ListaBiblioteca,
  type LivroBiblioteca,
  type PaginaBiblioteca,
} from '@/features/biblioteca/api';

export type Estante = {
  todos: LivroBiblioteca[];
  lendo: LivroBiblioteca[];
};

export type EstadoEstante =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; estante: Estante };

const LISTAS_DE_STATUS = ['lendo', 'desejo', 'lidos'] as const;

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar sua biblioteca.';
}

async function listaCompleta(
  lista: ListaBiblioteca,
  primeira: PaginaBiblioteca,
  signal: AbortSignal
) {
  const itens = [...primeira.itens];
  let cursor = primeira.proximoCursor;

  while (cursor) {
    const pagina = await buscarPaginaBiblioteca(lista, cursor, signal);
    itens.push(...pagina.itens);
    cursor = pagina.proximoCursor;
  }

  return itens;
}

async function carregarEstante(signal: AbortSignal): Promise<Estante> {
  const primeiras = await buscarBiblioteca(signal);
  const [lendo, desejo, lidos] = await Promise.all(
    LISTAS_DE_STATUS.map((lista) =>
      listaCompleta(lista, primeiras[lista], signal)
    )
  );

  return { todos: [...lendo, ...desejo, ...lidos], lendo };
}

export function useEstante() {
  const [estado, setEstado] = useState<EstadoEstante>({
    situacao: 'carregando',
  });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setEstado({ situacao: 'carregando' });

    carregarEstante(controller.signal)
      .then((estante) => {
        if (controller.signal.aborted) return;
        setEstado({ situacao: 'sucesso', estante });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setEstado({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });

    return () => controller.abort();
  }, [versao]);

  const recarregar = useCallback(() => setVersao((atual) => atual + 1), []);

  return { estado, recarregar };
}
