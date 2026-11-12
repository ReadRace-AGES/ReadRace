import { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { ApiError } from '@/api/client';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';

import { solicitarEntrada, type SituacaoClube } from './api';

const ERRO_SOLICITACAO = 'Não foi possível concluir. Tente novamente.';

export function EntradaClubeButton({
  clubeId,
  situacao,
  onPedido,
  onReconciliar,
}: {
  clubeId: string;
  situacao: SituacaoClube;
  onPedido: () => void;
  onReconciliar: () => void;
}) {
  const { showErrorToast } = useToastContext();
  const [enviando, setEnviando] = useState(false);
  const requisicao = useRef<AbortController | null>(null);
  useFocusEffect(
    useCallback(() => {
      requisicao.current = null;
      setEnviando(false);
      return () => requisicao.current?.abort();
    }, [clubeId])
  );

  async function pedir() {
    if (situacao !== 'nenhuma' || requisicao.current) return;
    const controller = new AbortController();
    requisicao.current = controller;
    setEnviando(true);
    try {
      await solicitarEntrada(clubeId, controller.signal);
      if (controller.signal.aborted) return;
      onPedido();
      showErrorToast('Pedido enviado');
    } catch (erro: unknown) {
      if (controller.signal.aborted) return;
      if (erro instanceof ApiError && erro.code === 'JA_E_MEMBRO')
        onReconciliar();
      showErrorToast(
        erro instanceof ApiError
          ? (erro.publicMessage ?? ERRO_SOLICITACAO)
          : ERRO_SOLICITACAO
      );
    } finally {
      if (!controller.signal.aborted) {
        requisicao.current = null;
        setEnviando(false);
      }
    }
  }

  return (
    <PrimaryButton
      label={
        situacao === 'membro'
          ? 'Participando'
          : situacao === 'pendente'
            ? 'Pedido enviado'
            : 'Pedir para entrar'
      }
      variant="outline"
      disabled={situacao !== 'nenhuma' || enviando}
      loading={enviando}
      onPress={pedir}
    />
  );
}
