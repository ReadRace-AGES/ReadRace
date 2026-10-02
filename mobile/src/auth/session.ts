import * as AuthSession from 'expo-auth-session';

import { configurarAutenticacao } from '@/api/client';

import { authHabilitada, COGNITO_CLIENT_ID, discovery } from './config';
import {
  apagarTokens,
  carregarTokens,
  salvarTokens,
  type Tokens,
} from './tokenStorage';

/**
 * Sessão do usuário, fora do React para o client HTTP conseguir usar. O AuthProvider acompanha as
 * mudanças por `onSessaoMudou`.
 */

const MARGEM_EXPIRACAO_MS = 60_000;

let atual: Tokens | null = null;
let renovando: Promise<string | null> | null = null;
const ouvintes = new Set<(logado: boolean) => void>();

export function onSessaoMudou(ouvinte: (logado: boolean) => void): () => void {
  ouvintes.add(ouvinte);
  return () => {
    ouvintes.delete(ouvinte);
  };
}

function avisar() {
  ouvintes.forEach((ouvinte) => ouvinte(atual !== null));
}

/** Lê os tokens salvos na abertura do app. */
export async function restaurarSessao(): Promise<boolean> {
  atual = await carregarTokens();
  return atual !== null;
}

export async function salvarRespostaDeToken(
  resposta: AuthSession.TokenResponse
): Promise<void> {
  atual = {
    accessToken: resposta.accessToken,
    // O Cognito não devolve um refresh token novo ao renovar: mantém o anterior.
    refreshToken: resposta.refreshToken ?? atual?.refreshToken ?? null,
    expiresAt: (resposta.issuedAt + (resposta.expiresIn ?? 3600)) * 1000,
  };
  await salvarTokens(atual);
  avisar();
}

/** Access token válido, renovado se estiver perto de expirar. `null` = sem sessão. */
export async function obterAccessToken(): Promise<string | null> {
  if (!authHabilitada || !atual) {
    return null;
  }
  if (atual.expiresAt - MARGEM_EXPIRACAO_MS > Date.now()) {
    return atual.accessToken;
  }
  return renovar();
}

/** Força a renovação (ex.: a API respondeu 401). Sem refresh token válido, encerra a sessão. */
export function renovar(): Promise<string | null> {
  // Requests simultâneos compartilham a mesma renovação.
  renovando ??= (async () => {
    try {
      if (!atual?.refreshToken) {
        throw new Error('Sessão sem refresh token.');
      }
      const resposta = await AuthSession.refreshAsync(
        { clientId: COGNITO_CLIENT_ID, refreshToken: atual.refreshToken },
        discovery
      );
      await salvarRespostaDeToken(resposta);
      return resposta.accessToken;
    } catch {
      await encerrarSessaoLocal();
      return null;
    } finally {
      renovando = null;
    }
  })();
  return renovando;
}

export async function encerrarSessaoLocal(): Promise<void> {
  atual = null;
  await apagarTokens();
  avisar();
}

/** Logout: revoga o refresh token no Cognito e limpa o aparelho. */
export async function sair(): Promise<void> {
  const refreshToken = atual?.refreshToken;
  if (refreshToken) {
    AuthSession.revokeAsync(
      { token: refreshToken, clientId: COGNITO_CLIENT_ID },
      discovery
    ).catch(() => {});
  }
  await encerrarSessaoLocal();
}

// Registra a sessão no client HTTP assim que este módulo é carregado (o _layout importa o
// AuthProvider, que importa este arquivo), antes de qualquer tela chamar a API.
configurarAutenticacao({
  obterToken: obterAccessToken,
  renovarToken: renovar,
  aoPerderSessao: encerrarSessaoLocal,
});
