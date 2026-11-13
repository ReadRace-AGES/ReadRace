import { useEffect, useMemo, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { ApiError } from '@/api/client';
import { PrimaryButton } from '@/components/PrimaryButton';
import { BottomSheet } from '@/components/BottomSheet';
import { useToastContext } from '@/components/toast-provider';
import { CloseIcon } from '@/features/desafios/DesafiarAmigoIcons';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import {
  registrarProgresso,
  ProgressoTimeoutError,
  type ProgressoLeituraResponse,
} from './api';
import { linhasDaRecompensa } from './recompensa';

const AVISO_PAGINA_ANTERIOR = 'Você já passou desta página. Ela não soma XP.';

type Destino =
  | { livroId: string; enviar?: never }
  | {
      livroId?: never;
      /** Envio próprio, para um livro que ainda não está na biblioteca (adicionar em "lendo"). */
      enviar: (pagina: number) => Promise<ProgressoLeituraResponse>;
    };

export type RegistrarProgressoSheetProps = Destino & {
  visible: boolean;
  totalPaginas: number;
  paginaAtual: number;
  /** Contexto da leitura. O cálculo de XP continua exclusivamente no backend. */
  paginaMaximaAlcancada?: number;
  onClose: () => void;
  onSuccess?: (progresso: ProgressoLeituraResponse) => void;
};

function CheckIcon({ color = colors.textInverse }: { color?: string }) {
  return (
    <Svg
      width={sizes.iconSmall}
      height={sizes.iconSmall}
      viewBox="0 0 16 16"
      fill="none"
    >
      <Path
        d="M13.333 4 6 11.333 2.667 8"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

// Livro com marcador antes do total de páginas, como no layout do `Adicionar progresso` 2060-1910:
// o glifo ocupa 12x15, por isso o viewBox recorta só a área dele.
function LivroIcon({ color = colors.text }: { color?: string }) {
  return (
    <Svg width={12} height={15} viewBox="4 2 16 20" fill="none">
      <Path
        d="M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 18H6V4h2v8l2.5-1.5L13 12V4h5v16z"
        fill={color}
      />
    </Svg>
  );
}

export function RegistrarProgressoSheet({
  visible,
  livroId,
  enviar,
  totalPaginas,
  paginaAtual,
  paginaMaximaAlcancada = paginaAtual,
  onClose,
  onSuccess,
}: RegistrarProgressoSheetProps) {
  const [page, setPage] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);
  const { showSuccessToast } = useToastContext();

  useEffect(() => {
    // Abre já na página de agora, e a barra mostra o percentual atual.
    if (visible) {
      setPage(paginaAtual > 0 ? String(paginaAtual) : '');
      setError(null);
    }
  }, [livroId, visible, paginaAtual]);

  const parsedPage = useMemo(() => {
    if (!/^\d+$/.test(page)) return null;
    const parsed = Number(page);
    return Number.isSafeInteger(parsed) && parsed > 0 && parsed <= totalPaginas
      ? parsed
      : null;
  }, [page, totalPaginas]);

  // O parsedPage vira null acima do total; aqui fica o número cru para avisar quanto o livro tem.
  const paginaDigitada = /^\d+$/.test(page) ? Number(page) : null;
  const passouDoTotal = paginaDigitada != null && paginaDigitada > totalPaginas;
  // Página anterior é aceita, só avisa antes de confirmar. O XP conta a partir da maior página
  // já alcançada, que pode estar à frente da atual.
  const paginaAnterior =
    parsedPage != null && parsedPage < paginaMaximaAlcancada;
  const aviso = passouDoTotal
    ? `O livro tem ${totalPaginas} páginas.`
    : paginaAnterior
      ? AVISO_PAGINA_ANTERIOR
      : null;

  const previewPage =
    parsedPage == null || Number.isNaN(parsedPage)
      ? 0
      : Math.min(Math.max(parsedPage, 0), totalPaginas);
  const progressPercent =
    totalPaginas > 0 ? Math.round((previewPage / totalPaginas) * 100) : 0;
  const canSubmit =
    parsedPage != null &&
    Number.isInteger(parsedPage) &&
    parsedPage > 0 &&
    parsedPage <= totalPaginas &&
    !loading;

  async function submit() {
    if (submitting.current || !canSubmit || parsedPage == null) return;

    submitting.current = true;
    setLoading(true);
    setError(null);
    try {
      const response = enviar
        ? await enviar(parsedPage)
        : await registrarProgresso(livroId!, parsedPage);
      onSuccess?.(response);
      // O painel é um Modal e ficaria por cima do aviso: fecha antes de mostrar a recompensa.
      onClose();
      showSuccessToast(linhasDaRecompensa(response));
    } catch (err) {
      setError(
        err instanceof ApiError || err instanceof ProgressoTimeoutError
          ? err.message
          : 'Não foi possível registrar o progresso. Tente novamente.'
      );
    } finally {
      submitting.current = false;
      setLoading(false);
    }
  }

  return (
    <BottomSheet
      visible={visible}
      dismissible={!loading}
      onClose={onClose}
      accessibilityLabel="Registrar progresso"
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <View style={styles.titleBlock}>
            <Text style={styles.title}>Registrar Progresso</Text>
            <Text style={styles.subtitle}>Em qual página você parou?</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={loading}
            onPress={onClose}
            hitSlop={spacing[3]}
            style={styles.closeButton}
          >
            <CloseIcon size={sizes.icon} color={colors.text} />
          </Pressable>
        </View>

        <TextInput
          accessibilityLabel="Página atual"
          editable={!loading}
          keyboardType="number-pad"
          value={page}
          onChangeText={(value) => {
            setPage(value);
            setError(null);
          }}
          placeholder="Ex: 145"
          placeholderTextColor={colors.surfacePinkStrong}
          style={styles.input}
        />

        <View style={styles.progressBlock}>
          <View style={styles.totalRow}>
            <LivroIcon />
            <Text style={styles.totalLabel}>/ {totalPaginas} páginas</Text>
          </View>
          <View style={styles.progressTrack}>
            <View
              style={[styles.progressFill, { width: `${progressPercent}%` }]}
            />
          </View>
          <Text style={styles.percentLabel}>{progressPercent}%</Text>
        </View>

        {aviso && (
          <Text accessibilityLiveRegion="polite" style={styles.aviso}>
            {aviso}
          </Text>
        )}

        {error && (
          <Text
            accessibilityRole="alert"
            accessibilityLiveRegion="polite"
            style={styles.error}
          >
            {error}
          </Text>
        )}

        <View style={styles.actions}>
          <PrimaryButton
            label="Atualizar Progresso"
            icon={CheckIcon}
            loading={loading}
            disabled={!canSubmit}
            onPress={submit}
          />
          <PrimaryButton
            label="Cancelar"
            variant="outline"
            disabled={loading}
            onPress={onClose}
          />
        </View>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing[5],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: spacing[4],
  },
  titleBlock: {
    flex: 1,
    gap: spacing[1],
  },
  title: {
    ...textStyles.h1,
    color: colors.primary,
  },
  subtitle: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
  },
  closeButton: {
    minWidth: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  input: {
    ...textStyles.display,
    height: spacing[10] * 2,
    borderWidth: sizes.borderWidth,
    borderColor: colors.primarySoft,
    borderRadius: radius.sm,
    color: colors.text,
    textAlign: 'center',
  },
  progressBlock: {
    alignItems: 'center',
    gap: spacing[4],
  },
  totalRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  totalLabel: {
    ...textStyles.bodyStrong,
    color: colors.text,
  },
  progressTrack: {
    alignSelf: 'stretch',
    height: sizes.progressTrackHeight,
    overflow: 'hidden',
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  progressFill: {
    height: '100%',
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
  },
  percentLabel: {
    ...textStyles.bodyStrong,
    color: colors.textSecondary,
  },
  aviso: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: 'center',
  },
  actions: {
    gap: spacing[2],
  },
});
