import { apiGet } from '@/api/client';
import type { Perfil } from '@/features/perfil/api';

export type Conquista = Perfil['conquistas'][number];

export function buscarMinhasConquistas(signal: AbortSignal) {
  return apiGet<Pick<Perfil, 'conquistas'>>('/api/me/perfil', {
    signal,
  }).then((perfil) => perfil.conquistas);
}
