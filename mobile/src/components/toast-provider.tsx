import { usePathname } from 'expo-router';
import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  type ReactNode,
} from 'react';

import { Toast, useToast } from './toast';

type ToastContextValue = {
  showToast: () => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps) {
  const { visible, show, hide } = useToast();
  const pathname = usePathname();

  // Se a rota mudar enquanto o toast está visível (ex.: usuário tocou em algo
  // sem destino e voltou logo em seguida), esconde na hora em vez de deixar
  // o timer de 2,5s terminar sozinho por cima da tela nova.
  useEffect(() => {
    hide();
  }, [pathname, hide]);

  const value = useMemo<ToastContextValue>(() => ({ showToast: show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast visible={visible} />
    </ToastContext.Provider>
  );
}

export function useToastContext(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error(
      'useToastContext precisa ser usado dentro de um ToastProvider'
    );
  }
  return context;
}

export default ToastProvider;