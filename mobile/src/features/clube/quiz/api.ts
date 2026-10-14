import { apiRequest } from '@/api/client';

export type PerguntaNova = {
  enunciado: string;
  /** Sempre as 4, nesta ordem — A, B, C, D. */
  alternativas: [string, string, string, string];
  /** Índice 0..3 da alternativa correta. */
  correta: number;
};

export type PublicarQuizRequest = {
  titulo: string;
  recompensaXp: number;
  perguntas: PerguntaNova[];
};

export type PublicarQuizResponse = {
  id: string;
  titulo: string;
  totalPerguntas: number;
};

/** `POST /api/clubes/{clubeId}/quiz` — publica de uma vez, substituindo o quiz ativo do clube. */
export function publicarQuiz(clubeId: string, request: PublicarQuizRequest) {
  return apiRequest<PublicarQuizResponse>(
    `/api/clubes/${encodeURIComponent(clubeId)}/quiz`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    }
  );
}