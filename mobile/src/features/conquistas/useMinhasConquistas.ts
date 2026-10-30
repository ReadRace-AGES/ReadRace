import { useEffect, useState } from 'react';
import { mensagemErroCarregamento } from '@/api/client';
import { buscarMinhasConquistas, type Conquista } from './api';

export type EstadoConquistas =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Conquista[] };

export function useMinhasConquistas() {
  const [estado, setEstado] = useState<EstadoConquistas>({
    situacao: 'carregando',
  });
  const [tentativa, setTentativa] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    const timeout = setTimeout(() => controller.abort(), 15000);
    const atualizar = (novo: EstadoConquistas) => {
      if (ativo) setEstado(novo);
    };
    atualizar({ situacao: 'carregando' });
    buscarMinhasConquistas(controller.signal)
      .then((dados) => atualizar({ situacao: 'sucesso', dados }))
      .catch((erro: unknown) =>
        atualizar({
          situacao: 'erro',
          mensagem: mensagemErroCarregamento(erro),
        })
      )
      .finally(() => clearTimeout(timeout));
    return () => {
      ativo = false;
      clearTimeout(timeout);
      controller.abort();
    };
  }, [tentativa]);
  return { estado, recarregar: () => setTentativa((valor) => valor + 1) };
}
