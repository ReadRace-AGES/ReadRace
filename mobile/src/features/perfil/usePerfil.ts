import { useEffect, useState } from 'react';
import { ApiError } from '@/api/client';
import { buscarPerfil, type Perfil } from './api';

export type EstadoPerfil =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Perfil };

export function usePerfil(usuarioId: string) {
  const [resultado, setResultado] = useState<{
    id: string;
    estado: EstadoPerfil;
  }>({ id: usuarioId, estado: { situacao: 'carregando' } });
  const [tentativa, setTentativa] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const timeout = setTimeout(() => controller.abort(), 15000);
    const atualizar = (estado: EstadoPerfil) => {
      if (ativo) setResultado({ id: usuarioId, estado });
    };
    atualizar({ situacao: 'carregando' });
    buscarPerfil(usuarioId, controller.signal)
      .then((dados) => atualizar({ situacao: 'sucesso', dados }))
      .catch((erro: unknown) =>
        atualizar({
          situacao: 'erro',
          mensagem:
            erro instanceof ApiError
              ? erro.message
              : 'Não foi possível carregar o perfil. Tente novamente.',
        })
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
  return { estado, recarregar: () => setTentativa((valor) => valor + 1) };
}
