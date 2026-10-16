import { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { ApiError } from '@/api/client';
import { BottomSheet } from '@/components/BottomSheet';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import { publicarPostDoClube, type PostForum } from './api';
import { textoPostValido } from './model';

type NovoPostSheetProps = {
  visible: boolean;
  clubeId: string;
  onClose: () => void;
  onSuccess: (post: PostForum) => void;
};

export function NovoPostSheet({
  visible,
  clubeId,
  onClose,
  onSuccess,
}: NovoPostSheetProps) {
  const [texto, setTexto] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef<AbortController | null>(null);
  const canSubmit = visible && textoPostValido(texto) && !loading;

  useEffect(() => {
    setTexto('');
    setError(null);
    setLoading(false);
    return () => {
      submitting.current?.abort();
      submitting.current = null;
    };
  }, [clubeId, visible]);

  function fechar() {
    if (!submitting.current) onClose();
  }

  async function submit() {
    if (submitting.current || !canSubmit) return;
    const controller = new AbortController();
    submitting.current = controller;
    setLoading(true);
    setError(null);
    try {
      const post = await publicarPostDoClube(clubeId, texto, controller.signal);
      if (controller.signal.aborted) return;
      onSuccess(post);
      setTexto('');
      onClose();
    } catch (err) {
      if (controller.signal.aborted) return;
      setError(
        err instanceof ApiError
          ? err.message
          : 'Não foi possível publicar o post. Tente novamente.'
      );
    } finally {
      if (submitting.current === controller) {
        submitting.current = null;
        setLoading(false);
      }
    }
  }

  return (
    <BottomSheet
      visible={visible}
      dismissible={!loading}
      onClose={fechar}
      accessibilityLabel="Novo post"
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Novo post</Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={loading}
            onPress={fechar}
            hitSlop={spacing[3]}
            style={styles.closeButton}
          >
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
        </View>
        <TextInput
          accessibilityLabel="Texto do post"
          placeholder="Escreva sobre a leitura..."
          placeholderTextColor={colors.textMuted}
          multiline
          editable={!loading}
          value={texto}
          onChangeText={(value) => {
            if (submitting.current) return;
            setTexto(value);
            setError(null);
          }}
          style={styles.input}
        />
        <Text style={styles.counter}>{texto.length}/2.000</Text>
        {error && (
          <Text
            accessibilityRole="alert"
            accessibilityLiveRegion="polite"
            style={styles.error}
          >
            {error}
          </Text>
        )}
        <PrimaryButton
          label="Publicar"
          loading={loading}
          disabled={!canSubmit}
          onPress={submit}
        />
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing[4] },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[4],
  },
  title: { ...textStyles.h1, color: colors.primary, flex: 1 },
  closeButton: {
    minWidth: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeText: { ...textStyles.h1, color: colors.text },
  input: {
    ...textStyles.body,
    minHeight: sizes.inputHeight * 3,
    maxHeight: sizes.inputHeight * 5,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.sm,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    color: colors.text,
    textAlignVertical: 'top',
  },
  counter: { ...textStyles.bodySmall, color: colors.textSecondary },
  error: { ...textStyles.bodySmall, color: colors.accent },
});
