import { usePathname } from 'expo-router';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  type ReactNode,
} from 'react';

import { Toast, useToast } from './toast';

type ToastContextValue = {
  showToast: () => void;
  showErrorToast: (message: string) => void;
  showToastAfterNavigation: (message: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps) {
  const { visible, message, show, hide } = useToast();
  const pathname = usePathname();

  const previousPathname = useRef(pathname);
  const pendingToast = useRef<string | null>(null);

  useEffect(() => {
    if (previousPathname.current === pathname) {
      return;
    }

    previousPathname.current = pathname;

    const pendingMessage = pendingToast.current;
    pendingToast.current = null;

    if (pendingMessage !== null) {
      show(pendingMessage);
    } else {
      hide();
    }
  }, [pathname, show, hide]);

  // Mantém a assinatura sem parâmetros usada pelas outras telas.
  const showToast = useCallback(() => show(), [show]);

  const showErrorToast = useCallback(
    (mensagem: string) => show(mensagem),
    [show]
  );

  // Exibe a mensagem quando a próxima mudança de rota acontecer.
  const showToastAfterNavigation = useCallback((mensagem: string) => {
    pendingToast.current = mensagem;
  }, []);

  const value = useMemo<ToastContextValue>(
    () => ({
      showToast,
      showErrorToast,
      showToastAfterNavigation,
    }),
    [showToast, showErrorToast, showToastAfterNavigation]
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
