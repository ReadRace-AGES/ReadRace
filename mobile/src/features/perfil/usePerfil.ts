import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/api/client';
import { buscarPerfil, type Perfil } from './api';

export type EstadoPerfil =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Perfil };

export function usePerfil(usuarioId?: string) {
  const [resultado, setResultado] = useState<{
    id: string | undefined;
    estado: EstadoPerfil;
  }>({ id: usuarioId, estado: { situacao: 'carregando' } });
  // Silenciosa é a busca de fundo ao voltar para a aba: quem já está vendo o perfil não perde o
  // conteúdo para o carregando, nem para um erro passageiro.
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });
  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const timeout = setTimeout(() => controller.abort(), 15000);
    const aplicar = (estado: EstadoPerfil, preservaConteudo: boolean) => {
      if (!ativo) return;
      setResultado((atual) =>
        preservaConteudo &&
        atual.id === usuarioId &&
        atual.estado.situacao === 'sucesso'
          ? atual
          : { id: usuarioId, estado }
      );
    };
    aplicar({ situacao: 'carregando' }, tentativa.silenciosa);
    buscarPerfil(usuarioId, controller.signal)
      .then((dados) => aplicar({ situacao: 'sucesso', dados }, false))
      .catch((erro: unknown) =>
        aplicar(
          {
            situacao: 'erro',
            mensagem:
              erro instanceof ApiError
                ? erro.message
                : 'Não foi possível carregar o perfil. Tente novamente.',
          },
          tentativa.silenciosa
        )
      )
      .finally(() => clearTimeout(timeout));
    return () => {
      ativo = false;
      clearTimeout(timeout);
      controller.abort();
    };
  }, [usuarioId, tentativa]);
  const estado: EstadoPerfil =
    resultado.id === usuarioId ? resultado.estado : { situacao: 'carregando' };
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
  return { estado, recarregar, atualizar };
}
