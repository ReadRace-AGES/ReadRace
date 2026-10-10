import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarCatalogo, type ResultadoCatalogo } from './api';

const ATRASO_DIGITACAO_MS = 300;
const MINIMO_CARACTERES = 3;

export type BuscaCatalogo =
  | { situacao: 'inicial' }
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; resultados: ResultadoCatalogo[] };

export type BuscaCatalogoState = {
  busca: BuscaCatalogo;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível buscar. Tente novamente.';
}

/** Busca no catálogo a partir de 3 caracteres, com debounce (#155). */
export function useBuscaCatalogo(termo: string): BuscaCatalogoState {
  const [busca, setBusca] = useState<BuscaCatalogo>({ situacao: 'inicial' });
  const [versao, setVersao] = useState(0);
  const termoLimpo = termo.trim();

  useEffect(() => {
    if (termoLimpo.length < MINIMO_CARACTERES) {
      setBusca({ situacao: 'inicial' });
      return;
    }

    setBusca({ situacao: 'carregando' });
    const controller = new AbortController();
    const timer = setTimeout(() => {
      buscarCatalogo(termoLimpo, controller.signal)
        .then((resultados) => {
          if (controller.signal.aborted) return;
          setBusca({ situacao: 'sucesso', resultados });
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
  }, [termoLimpo, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { busca, recarregar };
}
