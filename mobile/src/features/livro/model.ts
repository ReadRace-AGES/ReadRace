import type { LivroDetalhe } from './api';
import type { ProgressoLeituraResponse } from '../progresso/api';

/** O percentual é o do servidor; XP nunca entra no estado de apresentação. */
export function aplicarProgresso(
  detalhe: LivroDetalhe,
  resposta: ProgressoLeituraResponse
): LivroDetalhe {
  return {
    ...detalhe,
    livro: { ...detalhe.livro, totalPaginas: resposta.totalPaginas },
    progresso: {
      paginaAtual: resposta.paginaAtual,
      paginaMaximaAlcancada: resposta.paginaMaximaAlcancada,
      percentual: resposta.percentual,
      concluido: resposta.concluido,
    },
  };
}

export function tempoRelativo(data: string, agora = Date.now()): string {
  const timestamp = Date.parse(data);
  if (!Number.isFinite(timestamp)) return '';
  const minutos = Math.floor(Math.max(0, agora - timestamp) / 60000);
  if (minutos < 1) return 'agora';
  if (minutos < 60) return `${minutos}min atrás`;
  const horas = Math.floor(minutos / 60);
  return horas < 24 ? `${horas}h atrás` : `${Math.floor(horas / 24)}d atrás`;
}
