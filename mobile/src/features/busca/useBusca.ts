import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscar, type ResultadoBusca, type TipoBusca } from './api';

const ATRASO_DIGITACAO_MS = 300;

export type Busca =
  | { situacao: 'inicial' }
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: ResultadoBusca };

export type BuscaState = {
  busca: Busca;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível fazer a busca.';
}

export function useBusca(termo: string, tipo: TipoBusca): BuscaState {
  const [busca, setBusca] = useState<Busca>({ situacao: 'inicial' });
  const [versao, setVersao] = useState(0);
  const termoLimpo = termo.trim();

  useEffect(() => {
    if (!termoLimpo) {
      setBusca({ situacao: 'inicial' });
      return;
    }

    setBusca({ situacao: 'carregando' });
    const controller = new AbortController();
    const timer = setTimeout(() => {
      buscar(termoLimpo, tipo, controller.signal)
        .then((dados) => {
          if (controller.signal.aborted) return;
          setBusca({ situacao: 'sucesso', dados });
        })
        .catch((erro: unknown) => {
          if (controller.signal.aborted) return;
          setBusca({ situacao: 'erro', mensagem: mensagemDe(erro) });
        });
    }, ATRASO_DIGITACAO_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [termoLimpo, tipo, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { busca, recarregar };
}
