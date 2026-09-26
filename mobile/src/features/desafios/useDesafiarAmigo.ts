import { useCallback, useEffect, useRef, useState } from 'react';

import { ApiError } from '@/api/client';

import {
  buscarOponentes,
  criarDesafio,
  type CriarDesafioRequest,
  type Oponente,
} from './api';

const ATRASO_BUSCA_MS = 300;

export type EstadoOponentes =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; oponentes: Oponente[] };

export type EstadoEnvio =
  | { situacao: 'inicial' }
  | { situacao: 'enviando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso' };

function mensagemDe(erro: unknown, mensagemPadrao: string) {
  return erro instanceof ApiError ? erro.message : mensagemPadrao;
}

export function useOponentes(termo: string) {
  const [versao, setVersao] = useState(0);
  const termoLimpo = termo.trim();
  const [resultado, setResultado] = useState<{
    termo: string;
    versao: number;
    estado: EstadoOponentes;
  }>({ termo: termoLimpo, versao, estado: { situacao: 'carregando' } });
  const estado: EstadoOponentes =
    resultado.termo === termoLimpo && resultado.versao === versao
      ? resultado.estado
      : { situacao: 'carregando' };

  useEffect(() => {
    setResultado({
      termo: termoLimpo,
      versao,
      estado: { situacao: 'carregando' },
    });

    const controller = new AbortController();
    const atraso = termoLimpo ? ATRASO_BUSCA_MS : 0;

    const timer = setTimeout(() => {
      buscarOponentes(termoLimpo, controller.signal)
        .then(({ oponentes }) => {
          if (controller.signal.aborted) return;

          setResultado({
            termo: termoLimpo,
            versao,
            estado: { situacao: 'sucesso', oponentes },
          });
        })
        .catch((erro: unknown) => {
          if (controller.signal.aborted) return;

          setResultado({
            termo: termoLimpo,
            versao,
            estado: {
              situacao: 'erro',
              mensagem: mensagemDe(
                erro,
                'Não foi possível carregar os oponentes.'
              ),
            },
          });
        });
    }, atraso);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [termoLimpo, versao]);

  const recarregar = useCallback(() => {
    setVersao((atual) => atual + 1);
  }, []);

  return {
    estado,
    recarregar,
  };
}

export function useCriarDesafio() {
  const [estado, setEstado] = useState<EstadoEnvio>({
    situacao: 'inicial',
  });
  const enviandoRef = useRef(false);

  const enviar = useCallback(async (request: CriarDesafioRequest) => {
    if (enviandoRef.current) return false;

    enviandoRef.current = true;
    setEstado({ situacao: 'enviando' });

    try {
      await criarDesafio(request);
      setEstado({ situacao: 'sucesso' });
      return true;
    } catch (erro) {
      setEstado({
        situacao: 'erro',
        mensagem: mensagemDe(
          erro,
          'Não foi possível enviar o desafio. Tente novamente.'
        ),
      });
      return false;
    } finally {
      enviandoRef.current = false;
    }
  }, []);

  const limparErro = useCallback(() => {
    setEstado((atual) =>
      atual.situacao === 'erro' ? { situacao: 'inicial' } : atual
    );
  }, []);

  return {
    estado,
    enviar,
    limparErro,
  };
}
