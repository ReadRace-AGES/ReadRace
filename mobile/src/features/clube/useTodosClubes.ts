import { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { mensagemErroCarregamento } from '@/api/client';
import { buscarClubes, type ClubeDisponivel } from './api';

type Carga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; clubes: ClubeDisponivel[] };

export function useTodosClubes() {
  const [estado, setEstado] = useState<Carga>({ situacao: 'carregando' });
  const requisicao = useRef<AbortController | null>(null);
  const consultar = useCallback((silenciosa = false) => {
    requisicao.current?.abort();
    const controller = new AbortController();
    requisicao.current = controller;
    setEstado((atual) =>
      silenciosa && atual.situacao === 'sucesso'
        ? atual
        : { situacao: 'carregando' }
    );
    buscarClubes(controller.signal)
      .then((dados) => {
        if (!controller.signal.aborted)
          setEstado({ situacao: 'sucesso', clubes: dados.clubes });
      })
      .catch((erro: unknown) => {
        if (!controller.signal.aborted)
          setEstado((atual) =>
            silenciosa && atual.situacao === 'sucesso'
              ? atual
              : { situacao: 'erro', mensagem: mensagemErroCarregamento(erro) }
          );
      });
  }, []);
  useFocusEffect(
    useCallback(() => {
      consultar(true);
      return () => requisicao.current?.abort();
    }, [consultar])
  );
  const pedidoEnviado = useCallback((id: string) => {
    requisicao.current?.abort();
    setEstado((atual) =>
      atual.situacao === 'sucesso'
        ? {
            ...atual,
            clubes: atual.clubes.map((clube) =>
              clube.id === id ? { ...clube, situacao: 'pendente' } : clube
            ),
          }
        : atual
    );
  }, []);
  return { estado, recarregar: consultar, pedidoEnviado };
}
