import { usePathname } from 'expo-router';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  type ReactNode,
} from 'react';

import { Toast, useToast } from './toast';

type ToastContextValue = {
  showToast: () => void;
  /** Mensagem específica de erro (ex.: falha ao curtir) — não confundir com {@link showToast}. */
  showErrorToast: (message: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps) {
  const { visible, message, show, hide } = useToast();
  const pathname = usePathname();

  useEffect(() => {
    hide();
  }, [pathname, hide]);

  // Sem parâmetros de propósito: várias telas passam `showToast` direto para `onPress`, e um
  // evento de toque não pode virar mensagem do toast.
  const showToast = useCallback(() => show(), [show]);
  const showErrorToast = useCallback(
    (mensagem: string) => show(mensagem),
    [show]
  );

  const value = useMemo<ToastContextValue>(
    () => ({ showToast, showErrorToast }),
    [showToast, showErrorToast]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast visible={visible} message={message} />
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
