import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import Svg, { Path } from "react-native-svg";

import { ApiError } from "@/api/client";
import { BottomSheet } from "@/components/BottomSheet";
import { colors, radius, shadows, sizes, spacing, textStyles } from "@/theme";

import { registrarProgresso, type ProgressoLeituraResponse } from "./api";

export type RegistrarProgressoSheetProps = {
  visible: boolean;
  livroId: string;
  totalPaginas: number;
  paginaAtual: number;
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

export function RegistrarProgressoSheet({
  visible,
  livroId,
  totalPaginas,
  paginaAtual,
  onClose,
  onSuccess,
}: RegistrarProgressoSheetProps) {
  const [page, setPage] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      setPage(paginaAtual > 0 ? String(paginaAtual) : "");
      setError(null);
      setLoading(false);
    }
  }, [paginaAtual, visible]);

  const parsedPage = useMemo(() => {
    if (!page.trim()) return null;
    return Number(page);
  }, [page]);

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
    if (!canSubmit || parsedPage == null) return;

    setLoading(true);
    setError(null);
    try {
      const response = await registrarProgresso(livroId, parsedPage);
      onSuccess?.(response);
      onClose();
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Nao foi possivel registrar o progresso. Tente novamente.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <BottomSheet
      visible={visible}
      onClose={onClose}
      accessibilityLabel="Registrar progresso"
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <View style={styles.titleBlock}>
            <Text style={styles.title}>Registrar Progresso</Text>
            <Text style={styles.subtitle}>Em qual pagina voce parou?</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={loading}
            onPress={onClose}
            hitSlop={spacing[3]}
            style={styles.closeButton}
          >
            <Text style={styles.closeText}>x</Text>
          </Pressable>
        </View>

        <TextInput
          accessibilityLabel="Pagina atual"
          editable={!loading}
          keyboardType="number-pad"
          value={page}
          onChangeText={(value) => {
            setPage(value.replace(/\D/g, ""));
            setError(null);
          }}
          placeholder="Ex: 145"
          placeholderTextColor={colors.surfacePinkStrong}
          style={styles.input}
        />

        <View style={styles.progressBlock}>
          <Text style={styles.totalLabel}>/ {totalPaginas} paginas</Text>
          <View style={styles.progressTrack}>
            <View
              style={[styles.progressFill, { width: `${progressPercent}%` }]}
            />
          </View>
          <Text style={styles.percentLabel}>{progressPercent}%</Text>
        </View>

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
          <Pressable
            accessibilityRole="button"
            disabled={!canSubmit}
            onPress={submit}
            style={[
              styles.primaryButton,
              shadows.button,
              !canSubmit && styles.disabledButton,
            ]}
          >
            {loading ? (
              <ActivityIndicator color={colors.textInverse} />
            ) : (
              <>
                <CheckIcon />
                <Text style={styles.primaryButtonText}>
                  Atualizar Progresso
                </Text>
              </>
            )}
          </Pressable>
          <Pressable
            accessibilityRole="button"
            disabled={loading}
            onPress={onClose}
            style={styles.secondaryButton}
          >
            <Text style={styles.secondaryButtonText}>Cancelar</Text>
          </Pressable>
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
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
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
    alignItems: "center",
    justifyContent: "center",
  },
  closeText: {
    ...textStyles.h1,
    color: colors.text,
  },
  input: {
    ...textStyles.display,
    height: spacing[10] * 2,
    borderWidth: sizes.borderWidth,
    borderColor: colors.primarySoft,
    borderRadius: radius.sm,
    color: colors.text,
    textAlign: "center",
  },
  progressBlock: {
    alignItems: "center",
    gap: spacing[4],
  },
  totalLabel: {
    ...textStyles.bodyStrong,
    color: colors.text,
  },
  progressTrack: {
    alignSelf: "stretch",
    height: sizes.progressTrackHeight,
    overflow: "hidden",
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  progressFill: {
    height: "100%",
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
  },
  percentLabel: {
    ...textStyles.bodyStrong,
    color: colors.textSecondary,
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: "center",
  },
  actions: {
    gap: spacing[2],
  },
  primaryButton: {
    minHeight: sizes.buttonHeight + spacing[2],
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: spacing[2],
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
    paddingHorizontal: spacing[5],
  },
  disabledButton: {
    backgroundColor: colors.textMuted,
  },
  primaryButtonText: {
    ...textStyles.button,
    color: colors.textInverse,
  },
  secondaryButton: {
    minHeight: sizes.buttonHeight + spacing[2],
    alignItems: "center",
    justifyContent: "center",
    borderWidth: sizes.borderWidth,
    borderColor: colors.primary,
    borderRadius: radius.pill,
    paddingHorizontal: spacing[5],
  },
  secondaryButtonText: {
    ...textStyles.button,
    color: colors.text,
  },
});
