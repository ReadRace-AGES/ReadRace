import type { Conquista } from './api';

const doisDigitos = (valor: number) => String(valor).padStart(2, '0');

/** "DD/MM/AAAA" no fuso do aparelho — a API devolve o instante em UTC. */
export function formatarData(iso: string) {
  const data = new Date(iso);
  return `${doisDigitos(data.getDate())}/${doisDigitos(data.getMonth() + 1)}/${data.getFullYear()}`;
}

/**
 * Desbloqueadas da mais recente para a mais antiga; as próximas mantêm a ordem da API.
 */
export function separarConquistas(conquistas: Conquista[]) {
  const instante = (conquista: Conquista) =>
    conquista.data ? Date.parse(conquista.data) : 0;
  return {
    desbloqueadas: conquistas
      .filter((conquista) => conquista.desbloqueada)
      .sort((a, b) => instante(b) - instante(a)),
    proximas: conquistas.filter((conquista) => !conquista.desbloqueada),
  };
}
