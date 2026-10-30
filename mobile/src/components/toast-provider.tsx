import { usePathname } from 'expo-router';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import { Toast, useToast } from './toast';

type ToastContextValue = {
  showToast: () => void;
  /** Mensagem específica de erro (ex.: falha ao curtir) — não confundir com {@link showToast}. */
  showErrorToast: (message: string) => void;
  showToastAfterNavigation: (message: string) => void;
  setTabBarHeight: (height: number) => void;
};

const VALIDADE_DO_TOAST_PENDENTE_MS = 2000;

const ToastContext = createContext<ToastContextValue | null>(null);

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps) {
  const { visible, message, show, hide } = useToast();
  const pathname = usePathname();
  const [tabBarHeight, setTabBarHeight] = useState(0);

  const pendingToast = useRef<{ message: string; criadoEm: number } | null>(
    null
  );

  useEffect(() => {
    const pendente = pendingToast.current;
    pendingToast.current = null;
    if (
      pendente &&
      Date.now() - pendente.criadoEm <= VALIDADE_DO_TOAST_PENDENTE_MS
    ) {
      show(pendente.message);
      return;
    }
    hide();
  }, [pathname, show, hide]);

  // Sem parâmetros de propósito: várias telas passam `showToast` direto para `onPress`, e um
  // evento de toque não pode virar mensagem do toast.
  const showToast = useCallback(() => show(), [show]);
  const showErrorToast = useCallback(
    (mensagem: string) => show(mensagem),
    [show]
  );
  const showToastAfterNavigation = useCallback((mensagem: string) => {
    pendingToast.current = { message: mensagem, criadoEm: Date.now() };
  }, []);

  const value = useMemo<ToastContextValue>(
    () => ({
      showToast,
      showErrorToast,
      showToastAfterNavigation,
      setTabBarHeight,
    }),
    [showToast, showErrorToast, showToastAfterNavigation]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast visible={visible} message={message} tabBarHeight={tabBarHeight} />
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
