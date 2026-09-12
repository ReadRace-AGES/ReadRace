import { createContext, useContext, useMemo, type ReactNode } from 'react';

import { Toast, useToast } from './toast';

type ToastContextValue = {
  showToast: () => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps) {
  const { visible, show } = useToast();

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