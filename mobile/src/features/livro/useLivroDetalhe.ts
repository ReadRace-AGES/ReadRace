import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/api/client';
import type { ProgressoLeituraResponse } from '@/features/progresso/api';
import { buscarDetalhe, type LivroDetalhe } from './api';
import { aplicarProgresso } from './model';

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: LivroDetalhe };

export function useLivroDetalhe(livroId: string) {
  const [estado, setEstado] = useState<Estado>({ situacao: 'carregando' });
  const [tentativa, setTentativa] = useState(0);
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
  return {
    estado,
    recarregar: () => setTentativa((t) => t + 1),
    atualizarProgresso,
  };
}
