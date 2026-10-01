/**
 * Cliente HTTP minimo da API do ReadRace.
 *
 * Com login ligado, toda chamada envia o access token do Cognito (`Authorization: Bearer`) e o
 * backend descobre o usuário por ele. Sem login (backend em modo seed), nada é enviado e o backend
 * usa o usuário de seed. A URL base vem de `EXPO_PUBLIC_API_URL` (ver `mobile/.env.example`); sem
 * ela, assume o backend local na porta 8080.
 */
const BASE_URL = (process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/+$/, "");

/** Quem fornece o token de acesso. Registrado por `src/auth/session.ts`. */
export type ProvedorDeToken = {
  obterToken: () => Promise<string | null>;
  renovarToken: () => Promise<string | null>;
  aoPerderSessao: () => Promise<void>;
};

let provedorDeToken: ProvedorDeToken | null = null;

/**
 * O client não importa a sessão diretamente: assim continua carregável pelos testes em Node, que
 * não têm os módulos nativos do Expo.
 */
export function configurarAutenticacao(provedor: ProvedorDeToken | null): void {
  provedorDeToken = provedor;
}

/** Envelope padrão de erro do backend (#10): `code` estável, `message` exibível. */
export type ApiErrorBody = {
  code: string;
  message: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message ?? `Erro ${status} ao chamar a API.`);
    this.name = "ApiError";
    this.status = status;
    this.code = body.code ?? "UNKNOWN";
  }
}

export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(path, {
    ...init,
    method: init?.method ?? "GET",
    headers: { Accept: "application/json", ...init?.headers },
  });
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (typeof init.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const token = provedorDeToken ? await provedorDeToken.obterToken() : null;
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
  });

  // Token revogado ou expirado antes do previsto: renova uma vez e repete a chamada.
  if (response.status === 401 && token && provedorDeToken) {
    const novoToken = await provedorDeToken.renovarToken();
    if (novoToken) {
      headers.set("Authorization", `Bearer ${novoToken}`);
      response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
    }
    if (response.status === 401) {
      // Volta para a tela de login.
      await provedorDeToken.aoPerderSessao();
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorBody(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function readErrorBody(response: Response): Promise<Partial<ApiErrorBody>> {
  try {
    return (await response.json()) as Partial<ApiErrorBody>;
  } catch {
    // Corpo não-JSON (proxy, gateway): a mensagem padrão do ApiError cobre.
    return {};
  }
}
