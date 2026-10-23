import { apiGet } from '@/api/client';

export type TipoLista = 'seguidores' | 'seguindo';
export type Leitor = {
  id: string;
  nome: string;
  nomeUsuario: string;
  avatarUrl: string | null;
};

export function buscarLista(
  usuarioId: string,
  tipo: TipoLista,
  signal: AbortSignal
) {
  return apiGet<{ usuarios: Leitor[] }>(
    `/api/usuarios/${encodeURIComponent(usuarioId)}/${tipo}`,
    { signal }
  );
}
