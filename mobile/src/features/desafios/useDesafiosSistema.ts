import { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { mensagemErroCarregamento } from '@/api/client';

import {
  consultarDesafiosSistema,
  type DesafiosSistemaResponse,
} from './sistemaApi';

type EstadoSistema =
  | { situacao: 'carregando' }
  | { situacao: 'sucesso'; desafios: DesafiosSistemaResponse }
  | { situacao: 'erro'; mensagem: string };

export function useDesafiosSistema() {
  const [estado, setEstado] = useState<EstadoSistema>({
    situacao: 'carregando',
  });
  const requisicao = useRef<AbortController | null>(null);
  const emFoco = useRef(false);

  const recarregar = useCallback(async () => {
    if (!emFoco.current) return;

    requisicao.current?.abort();
    const controller = new AbortController();
    requisicao.current = controller;
    setEstado({ situacao: 'carregando' });

    try {
      const desafios = await consultarDesafiosSistema(controller.signal);
      if (controller.signal.aborted || requisicao.current !== controller)
        return;
      setEstado({ situacao: 'sucesso', desafios });
    } catch (erro: unknown) {
      if (controller.signal.aborted || requisicao.current !== controller)
        return;
      setEstado({
        situacao: 'erro',
        mensagem: mensagemErroCarregamento(erro),
      });
    } finally {
      if (requisicao.current === controller) requisicao.current = null;
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      emFoco.current = true;
      void recarregar();
      return () => {
        emFoco.current = false;
        requisicao.current?.abort();
        requisicao.current = null;
      };
    }, [recarregar])
  );

  return { estado, recarregar };
}
