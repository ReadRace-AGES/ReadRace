import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export type Tokens = {
  accessToken: string;
  refreshToken: string | null;
  /** Epoch em ms. */
  expiresAt: number;
};

// Cada token numa chave separada: o SecureStore tem limite de tamanho por valor.
const K_ACCESS = 'rr.accessToken';
const K_REFRESH = 'rr.refreshToken';
const K_EXPIRES = 'rr.expiresAt';

// O SecureStore não existe no web; lá os tokens ficam no localStorage.
const web = {
  get(chave: string): string | null {
    try {
      return localStorage.getItem(chave);
    } catch {
      return null;
    }
  },
  set(chave: string, valor: string): void {
    try {
      localStorage.setItem(chave, valor);
    } catch {}
  },
  del(chave: string): void {
    try {
      localStorage.removeItem(chave);
    } catch {}
  },
};

const ehWeb = Platform.OS === 'web';

async function ler(chave: string): Promise<string | null> {
  return ehWeb ? web.get(chave) : SecureStore.getItemAsync(chave);
}

async function gravar(chave: string, valor: string): Promise<void> {
  return ehWeb ? web.set(chave, valor) : SecureStore.setItemAsync(chave, valor);
}

async function apagar(chave: string): Promise<void> {
  return ehWeb ? web.del(chave) : SecureStore.deleteItemAsync(chave);
}

export async function carregarTokens(): Promise<Tokens | null> {
  const [accessToken, refreshToken, expiresAt] = await Promise.all([
    ler(K_ACCESS),
    ler(K_REFRESH),
    ler(K_EXPIRES),
  ]);
  if (!accessToken || !expiresAt) {
    return null;
  }
  return { accessToken, refreshToken, expiresAt: Number(expiresAt) };
}

export async function salvarTokens(tokens: Tokens): Promise<void> {
  await Promise.all([
    gravar(K_ACCESS, tokens.accessToken),
    tokens.refreshToken
      ? gravar(K_REFRESH, tokens.refreshToken)
      : apagar(K_REFRESH),
    gravar(K_EXPIRES, String(tokens.expiresAt)),
  ]);
}

export async function apagarTokens(): Promise<void> {
  await Promise.all([apagar(K_ACCESS), apagar(K_REFRESH), apagar(K_EXPIRES)]);
}
