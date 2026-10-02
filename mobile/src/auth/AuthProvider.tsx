import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import { authHabilitada } from './config';
import { onSessaoMudou, restaurarSessao, sair } from './session';

type Status = 'carregando' | 'logado' | 'anonimo';

type AuthContextValue = {
  status: Status;
  sair: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/** Sem as variáveis do Cognito, o app se comporta como logado (backend em modo seed). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>(
    authHabilitada ? 'carregando' : 'logado'
  );

  useEffect(() => {
    if (!authHabilitada) {
      return;
    }
    const cancelar = onSessaoMudou((logado) =>
      setStatus(logado ? 'logado' : 'anonimo')
    );
    restaurarSessao().then((logado) =>
      setStatus(logado ? 'logado' : 'anonimo')
    );
    return cancelar;
  }, []);

  return (
    <AuthContext.Provider value={{ status, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth precisa estar dentro de <AuthProvider>.');
  }
  return contexto;
}
