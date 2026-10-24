
import { useRouter, type Href } from 'expo-router';

import { buscarUsuarioAtual } from '@/features/perfil/api';

type Router = ReturnType<typeof useRouter>;

export async function abrirPerfilDoRanking(
  usuarioId: string,
  router: Router
): Promise<void> {
  const controller = new AbortController();

  const usuarioAtual = await buscarUsuarioAtual(controller.signal);

  if (usuarioId === usuarioAtual.id) {
    router.navigate('/(tabs)/(perfil)/perfil');
    return;
  }

  router.push({
    pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
    params: { usuarioId },
  } as unknown as Href);
}
