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
  const [versao, setVersao] = useState(0);
  const termoLimpo = termo.trim();
  const [resultado, setResultado] = useState<{
    termo: string;
    tipo: TipoBusca;
    versao: number;
    busca: Busca;
  }>({ termo: termoLimpo, tipo, versao, busca: { situacao: 'inicial' } });
  const busca: Busca = !termoLimpo
    ? { situacao: 'inicial' }
    : resultado.termo === termoLimpo &&
        resultado.tipo === tipo &&
        resultado.versao === versao
      ? resultado.busca
      : { situacao: 'carregando' };

  useEffect(() => {
    if (!termoLimpo) {
      setResultado({
        termo: termoLimpo,
        tipo,
        versao,
        busca: { situacao: 'inicial' },
      });
      return;
    }

    setResultado({
      termo: termoLimpo,
      tipo,
      versao,
      busca: { situacao: 'carregando' },
    });
    const controller = new AbortController();
    const timer = setTimeout(() => {
      buscar(termoLimpo, tipo, controller.signal)
        .then((dados) => {
          if (controller.signal.aborted) return;
          setResultado({
            termo: termoLimpo,
            tipo,
            versao,
            busca: { situacao: 'sucesso', dados },
          });
        })
        .catch((erro: unknown) => {
          if (controller.signal.aborted) return;
          setResultado({
            termo: termoLimpo,
            tipo,
            versao,
            busca: { situacao: 'erro', mensagem: mensagemDe(erro) },
          });
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
