import { apiGet, apiRequest } from '@/api/client';

/**
 * Quiz do clube (#162). As alternativas nunca dizem qual é a correta: quem decide se o membro
 * acertou é a API, na resposta. `pontosGanhos` são `Pontos` do clube e `resumo.xp` é `XP` do
 * usuário — as duas moedas nunca se convertem.
 */
export type Quiz = {
  id: string;
  titulo: string;
  livro: { titulo: string };
  recompensaXp: number;
  totalPerguntas: number;
  /** `null` quando o membro já respondeu todas. */
  proximaPergunta: PerguntaQuiz | null;
};

export type PerguntaQuiz = {
  id: string;
  numero: number;
  enunciado: string;
  alternativas: AlternativaQuiz[];
};

export type AlternativaQuiz = { id: string; letra: string; texto: string };

export type RespostaQuiz = {
  acertou: boolean;
  pontosGanhos: number;
  concluiu: boolean;
  /** Só vem na resposta que concluiu o quiz. */
  resumo: ResumoQuiz | null;
};

export type ResumoQuiz = {
  acertos: number;
  total: number;
  pontos: number;
  xp: number;
};

/** Códigos do envelope de erro que a tela trata pelo nome. */
export const ERRO_QUIZ = {
  semQuiz: 'QUIZ_NAO_ENCONTRADO',
  soMembro: 'SO_MEMBRO_RESPONDE',
  jaRespondida: 'PERGUNTA_JA_RESPONDIDA',
} as const;

function urlDoQuiz(clubeId: string) {
  return `/api/clubes/${encodeURIComponent(clubeId)}/quiz`;
}

/** `GET /api/clubes/{clubeId}/quiz` */
export function buscarQuiz(clubeId: string, signal?: AbortSignal) {
  return apiGet<Quiz>(urlDoQuiz(clubeId), { signal });
}

/** `POST /api/clubes/{clubeId}/quiz/perguntas/{perguntaId}/resposta` */
export function responderPergunta(
  clubeId: string,
  perguntaId: string,
  alternativaId: string
) {
  return apiRequest<RespostaQuiz>(
    `${urlDoQuiz(clubeId)}/perguntas/${encodeURIComponent(perguntaId)}/resposta`,
    { method: 'POST', body: JSON.stringify({ alternativaId }) }
  );
}
