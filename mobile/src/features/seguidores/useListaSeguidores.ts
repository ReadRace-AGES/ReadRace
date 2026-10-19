import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/api/client';
import { buscarPerfil, buscarUsuarioAtual } from '@/features/perfil/api';
import { buscarLista, type Leitor, type TipoLista } from './api';

type Carga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; nome: string; eu: string; usuarios: Leitor[] };

export function useListaSeguidores(usuarioId: string, tipo: TipoLista) {
  const chave = `${usuarioId}/${tipo}`;
  const [resultado, setResultado] = useState<{ chave: string; carga: Carga }>({
    chave,
    carga: { situacao: 'carregando' },
  });
  const [versao, setVersao] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    const aplicar = (carga: Carga) => {
      if (!controller.signal.aborted) setResultado({ chave, carga });
    };
    aplicar({ situacao: 'carregando' });
    Promise.all([
      buscarLista(usuarioId, tipo, controller.signal),
      buscarPerfil(usuarioId, controller.signal),
      buscarUsuarioAtual(controller.signal),
    ])
      .then(([lista, perfil, eu]) => {
        aplicar({
          situacao: 'sucesso',
          nome: perfil.nome,
          eu: eu.id,
          usuarios: lista.usuarios,
        });
      })
      .catch((erro: unknown) => {
        aplicar({
          situacao: 'erro',
          mensagem:
            erro instanceof ApiError
              ? erro.message
              : 'Não foi possível carregar a lista. Tente novamente.',
        });
      });
    return () => controller.abort();
  }, [usuarioId, tipo, chave, versao]);
  const recarregar = useCallback(() => setVersao((v) => v + 1), []);
  const carga: Carga =
    resultado.chave === chave ? resultado.carga : { situacao: 'carregando' };
  return { carga, recarregar };
}
