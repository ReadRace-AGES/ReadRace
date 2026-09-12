import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarBiblioteca, type BibliotecaResponse } from './api';

/** Estado da chamada: a tela nunca mostra dado parcial nem fica presa carregando. */
export type Carga<T> =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: T };

export type BibliotecaState = {
  biblioteca: Carga<BibliotecaResponse>;
  /** Refaz a chamada; é a ação de "tentar de novo". */
  recarregar: () => void;
};

// Só a mensagem do envelope da API é exibível (#10); erro de rede vira o texto padrão.
function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar sua biblioteca.';
}

/** Carrega a biblioteca do usuário atual; uma chamada, uma carga. */
export function useBiblioteca(): BibliotecaState {
  const [biblioteca, setBiblioteca] = useState<Carga<BibliotecaResponse>>({
    situacao: 'carregando',
  });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setBiblioteca({ situacao: 'carregando' });
    buscarBiblioteca(controller.signal)
      .then((dados) => setBiblioteca({ situacao: 'sucesso', dados }))
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setBiblioteca({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => controller.abort();
  }, [versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { biblioteca, recarregar };
}
