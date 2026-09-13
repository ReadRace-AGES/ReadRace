export type ApiErrorPayload = {
  code?: string;
  message?: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, payload?: ApiErrorPayload) {
    super(payload?.message || "Nao foi possivel concluir a acao.");
    this.name = "ApiError";
    this.status = status;
    this.code = payload?.code;
  }
}

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  });

  const text = await response.text();
  const body = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    throw new ApiError(response.status, body);
  }

  return body as T;
}
