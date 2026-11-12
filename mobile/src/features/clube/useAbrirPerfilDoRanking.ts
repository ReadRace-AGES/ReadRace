import { useCallback, useRef } from 'react';
import { useFocusEffect, useRouter } from 'expo-router';

import { useToastContext } from '@/components/toast-provider';
import { buscarUsuarioAtual } from '@/features/perfil/api';

/** Os dois rankings usam a mesma regra para abrir outro leitor ou a aba Perfil. */
export function useAbrirPerfilDoRanking(clubeId: string) {
  const router = useRouter();
  const { showErrorToast } = useToastContext();
  const consulta = useRef<AbortController | null>(null);

  useFocusEffect(
    useCallback(
      () => () => {
        consulta.current?.abort();
        consulta.current = null;
      },
      [clubeId]
    )
  );

  return useCallback(
    async (usuarioId: string) => {
      if (consulta.current) return;
      const controller = new AbortController();
      consulta.current = controller;
      const timeout = setTimeout(() => controller.abort(), 15000);
      try {
        const usuarioAtual = await buscarUsuarioAtual(controller.signal);
        if (controller.signal.aborted) return;
        if (usuarioId === usuarioAtual.id) {
          router.navigate('/(tabs)/(perfil)/perfil');
        } else {
          router.push({
            pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
            params: { usuarioId },
          });
        }
      } catch {
        if (consulta.current === controller) {
          showErrorToast('Não foi possível abrir o perfil. Tente novamente.');
        }
      } finally {
        clearTimeout(timeout);
        if (consulta.current === controller) consulta.current = null;
      }
    },
    [router, showErrorToast]
  );
}
