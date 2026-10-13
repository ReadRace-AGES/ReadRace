import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/api/client';
import {
  alterarSeguir,
  buscarPerfil,
  buscarUsuarioAtual,
  type Perfil,
} from './api';

export type EstadoPerfil =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Perfil; proprio: boolean };

export function usePerfil(usuarioId?: string) {
  const versao = useRef(0);
  const mutacao = useRef<AbortController | null>(null);
  const [alterandoId, setAlterandoId] = useState<string>();
  useEffect(() => {
    setAlterandoId(undefined);
    return () => {
      versao.current++;
      mutacao.current?.abort();
      mutacao.current = null;
    };
  }, [usuarioId]);
  const [resultado, setResultado] = useState<{
    id: string | undefined;
    estado: EstadoPerfil;
  }>({ id: usuarioId, estado: { situacao: 'carregando' } });
  // Silenciosa é a busca de fundo ao voltar para a aba: quem já está vendo o perfil não perde o
  // conteúdo para o carregando, nem para um erro passageiro.
  const [tentativa, setTentativa] = useState({ numero: 0, silenciosa: false });
  useEffect(() => {
    // O foco pode ter agendado a recarga no mesmo ciclo em que o leitor tocou no botão.
    if (mutacao.current) return;
    const controller = new AbortController();
    const versaoDaBusca = ++versao.current;
    let ativo = true;
    const timeout = setTimeout(() => controller.abort(), 15000);
    const aplicar = (estado: EstadoPerfil, preservaConteudo: boolean) => {
      if (!ativo || versao.current !== versaoDaBusca) return;
      setResultado((atual) =>
        preservaConteudo &&
        atual.id === usuarioId &&
        atual.estado.situacao === 'sucesso'
          ? atual
          : { id: usuarioId, estado }
      );
    };
    aplicar({ situacao: 'carregando' }, tentativa.silenciosa);
    Promise.all([
      buscarPerfil(usuarioId, controller.signal),
      usuarioId === undefined
        ? Promise.resolve(undefined)
        : buscarUsuarioAtual(controller.signal),
    ])
      .then(([dados, eu]) =>
        aplicar(
          {
            situacao: 'sucesso',
            dados,
            proprio: usuarioId === undefined || dados.id === eu?.id,
          },
          false
        )
      )
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
  const recarregar = useCallback(() => {
    if (!mutacao.current)
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: false }));
  }, []);
  const atualizar = useCallback(() => {
    if (!mutacao.current)
      setTentativa(({ numero }) => ({ numero: numero + 1, silenciosa: true }));
  }, []);
  const alternarSeguir = async () => {
    if (
      mutacao.current ||
      estado.situacao !== 'sucesso' ||
      estado.proprio ||
      usuarioId === undefined
    )
      return;
    const anterior = estado;
    const controller = new AbortController();
    mutacao.current = controller;
    // Uma leitura iniciada antes do toque não pode sobrescrever a alteração otimista.
    versao.current++;
    setAlterandoId(usuarioId);
    setResultado({
      id: usuarioId,
      estado: {
        ...anterior,
        dados: {
          ...anterior.dados,
          sigo: !anterior.dados.sigo,
          seguidores:
            anterior.dados.seguidores + (anterior.dados.sigo ? -1 : 1),
        },
      },
    });
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const relacao = await alterarSeguir(
        usuarioId,
        !anterior.dados.sigo,
        controller.signal
      );
      if (mutacao.current !== controller) return;
      setResultado({
        id: usuarioId,
        estado: {
          ...anterior,
          dados: { ...anterior.dados, ...relacao },
        },
      });
    } catch (erro) {
      if (mutacao.current !== controller) return;
      setResultado({ id: usuarioId, estado: anterior });
      throw new Error(
        erro instanceof ApiError
          ? erro.message
          : 'Não foi possível atualizar quem você segue. Tente novamente.'
      );
    } finally {
      clearTimeout(timeout);
      if (mutacao.current === controller) {
        mutacao.current = null;
        setAlterandoId(undefined);
      }
    }
  };
  return {
    estado,
    recarregar,
    atualizar,
    alternarSeguir,
    proprio:
      usuarioId === undefined ||
      (estado.situacao === 'sucesso' && estado.proprio),
    alterandoSeguir: alterandoId !== undefined && alterandoId === usuarioId,
  };
}
