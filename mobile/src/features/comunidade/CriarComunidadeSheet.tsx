import { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';

import { ApiError } from '@/api/client';
import { BottomSheet } from '@/components/BottomSheet';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import { criarComunidade, type ComunidadeCriada } from './api';
import {
  erroImagemComunidade,
  erroNomeComunidade,
  LIMITES_COMUNIDADE,
} from './criacao';

type Props = {
  visible: boolean;
  onClose: () => void;
  onSuccess: (comunidade: ComunidadeCriada) => void;
};

export function CriarComunidadeSheet({ visible, onClose, onSuccess }: Props) {
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [imagemUrl, setImagemUrl] = useState('');
  const [mostrarImagem, setMostrarImagem] = useState(false);
  const [imagemComErro, setImagemComErro] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef<AbortController | null>(null);
  const erroNome = erroNomeComunidade(nome);
  const erroImagem = erroImagemComunidade(imagemUrl);
  const podeCriar =
    visible &&
    !loading &&
    !erroNome &&
    !erroImagem &&
    descricao.trim().length <= LIMITES_COMUNIDADE.descricao;
  const imagemFalhou = imagemComErro === imagemUrl.trim();
  const capa =
    !erroImagem && imagemUrl.trim() && !imagemFalhou ? imagemUrl.trim() : null;

  useEffect(() => {
    if (visible) {
      setNome('');
      setDescricao('');
      setImagemUrl('');
      setMostrarImagem(false);
      setImagemComErro(null);
      setLoading(false);
      setError(null);
    }
    return () => {
      submitting.current?.abort();
      submitting.current = null;
    };
  }, [visible]);

  function fechar() {
    if (!submitting.current) onClose();
  }

  async function submit() {
    if (!podeCriar || submitting.current) return;
    const controller = new AbortController();
    submitting.current = controller;
    setLoading(true);
    setError(null);
    let criada: ComunidadeCriada;
    try {
      criada = await criarComunidade(
        {
          nome: nome.trim(),
          descricao: descricao.trim() || null,
          imagemUrl: imagemUrl.trim() || null,
        },
        controller.signal
      );
    } catch (err) {
      if (!controller.signal.aborted)
        setError(
          (err instanceof ApiError ? err.publicMessage : null) ??
            'Não foi possível criar a comunidade. Tente novamente.'
        );
      return;
    } finally {
      if (submitting.current === controller) {
        submitting.current = null;
        setLoading(false);
      }
    }
    if (!controller.signal.aborted) onSuccess(criada);
  }

  return (
    <BottomSheet
      visible={visible}
      onClose={fechar}
      dismissible={!loading}
      accessibilityLabel="Criar comunidade"
      footer={
        <View style={styles.footer}>
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
            label="Criar comunidade"
            onPress={submit}
            loading={loading}
            disabled={!podeCriar}
          />
          <Text style={styles.footerHint}>
            Você será o primeiro membro e administrador.
          </Text>
        </View>
      }
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <View style={styles.headerCopy}>
            <Text style={styles.eyebrow}>UM NOVO CAPÍTULO, EM COMPANHIA</Text>
            <Text accessibilityRole="header" style={styles.title}>
              Crie sua comunidade
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            onPress={fechar}
            disabled={loading}
            style={styles.close}
          >
            <Text style={styles.closeText}>×</Text>
          </Pressable>
        </View>
        <Text style={styles.subtitle}>
          Reúna leitores, compartilhe descobertas e dê espaço a boas conversas.
        </Text>

        <View style={styles.preview}>
          <LinearGradient
            colors={[colors.primary, colors.primarySoft]}
            style={StyleSheet.absoluteFill}
          />
          {capa && (
            <Image
              key={capa}
              source={{ uri: capa }}
              contentFit="cover"
              style={StyleSheet.absoluteFill}
              onError={() => setImagemComErro(capa)}
              accessibilityLabel="Prévia da capa da comunidade"
            />
          )}
          <View style={styles.previewContent}>
            <View style={styles.previewIcon}>
              <BookIcon color={colors.textInverse} size={sizes.icon} />
            </View>
            <View style={styles.previewText}>
              <Text style={styles.previewTag}>COMUNIDADE DE LEITORES</Text>
              <Text numberOfLines={2} style={styles.previewTitle}>
                {nome.trim() || 'Sua próxima boa conversa'}
              </Text>
              <Text style={styles.previewSubtitle}>
                Um interesse em comum. Muitas histórias.
              </Text>
            </View>
          </View>
        </View>

        <View style={styles.field}>
          <View style={styles.labelRow}>
            <Text style={styles.label}>
              Nome da comunidade <Text style={styles.required}>*</Text>
            </Text>
            <Text style={styles.counter}>
              {nome.length}/{LIMITES_COMUNIDADE.nome}
            </Text>
          </View>
          <TextInput
            accessibilityLabel="Nome da comunidade"
            placeholder="Ex.: Entre páginas e cafés"
            placeholderTextColor={colors.textSecondary}
            value={nome}
            maxLength={LIMITES_COMUNIDADE.nome}
            editable={!loading}
            onChangeText={(valor) => {
              if (!submitting.current) {
                setNome(valor);
                setError(null);
              }
            }}
            style={styles.input}
          />
          {!!nome && erroNome && <Text style={styles.error}>{erroNome}</Text>}
        </View>
        <View style={styles.field}>
          <View style={styles.labelRow}>
            <Text style={styles.label}>
              Sobre a comunidade <Text style={styles.optional}>opcional</Text>
            </Text>
            <Text style={styles.counter}>
              {descricao.length}/{LIMITES_COMUNIDADE.descricao}
            </Text>
          </View>
          <TextInput
            accessibilityLabel="Descrição da comunidade"
            placeholder="Que histórias e assuntos conectam vocês?"
            placeholderTextColor={colors.textSecondary}
            value={descricao}
            multiline
            maxLength={LIMITES_COMUNIDADE.descricao}
            editable={!loading}
            onChangeText={(valor) => {
              if (!submitting.current) {
                setDescricao(valor);
                setError(null);
              }
            }}
            style={[styles.input, styles.description]}
          />
        </View>
        {mostrarImagem ? (
          <View style={styles.field}>
            <Text style={styles.label}>
              Link da capa <Text style={styles.optional}>opcional</Text>
            </Text>
            <TextInput
              accessibilityLabel="Link da capa"
              placeholder="https://exemplo.com/capa.jpg"
              placeholderTextColor={colors.textSecondary}
              value={imagemUrl}
              maxLength={LIMITES_COMUNIDADE.imagemUrl}
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              editable={!loading}
              onChangeText={(valor) => {
                if (!submitting.current) {
                  setImagemUrl(valor);
                  setImagemComErro(null);
                  setError(null);
                }
              }}
              style={styles.input}
            />
            {erroImagem ? (
              <Text accessibilityRole="alert" style={styles.error}>
                {erroImagem}
              </Text>
            ) : (
              <Text style={styles.hint}>
                {imagemFalhou
                  ? 'A imagem não carregou. Confira o link ou use a capa padrão.'
                  : 'Use um link direto para uma imagem. A capa será recortada para preencher o espaço.'}
              </Text>
            )}
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Usar capa padrão"
              disabled={loading}
              onPress={() => {
                if (!submitting.current) {
                  setImagemUrl('');
                  setImagemComErro(null);
                  setMostrarImagem(false);
                }
              }}
              style={styles.linkButton}
            >
              <Text style={styles.link}>Usar capa padrão</Text>
            </Pressable>
          </View>
        ) : (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Adicionar capa por link"
            disabled={loading}
            onPress={() => setMostrarImagem(true)}
            style={styles.imageAction}
          >
            <Text style={styles.imageActionPlus}>+</Text>
            <View style={styles.headerCopy}>
              <Text style={styles.link}>Personalizar capa</Text>
              <Text style={styles.hint}>
                Adicione uma imagem por link, se quiser.
              </Text>
            </View>
          </Pressable>
        )}
        <View style={styles.note}>
          <Text style={styles.noteText}>Aberta a novas histórias</Text>
          <Text style={styles.hint}>
            Qualquer leitor pode encontrar e seguir sua comunidade.
          </Text>
        </View>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing[4], paddingBottom: spacing[4] },
  header: { flexDirection: 'row', alignItems: 'center', gap: spacing[2] },
  headerCopy: { flex: 1, gap: spacing[1] },
  eyebrow: { ...textStyles.microStrong, color: colors.primary },
  title: { ...textStyles.h1, color: colors.primary },
  subtitle: { ...textStyles.bodySmall, color: colors.textSecondary },
  close: {
    minHeight: sizes.buttonHeight,
    minWidth: sizes.buttonHeight,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: colors.surfaceMuted,
  },
  closeText: { ...textStyles.h1, color: colors.text },
  preview: {
    minHeight: spacing[10] * 3,
    overflow: 'hidden',
    borderRadius: radius.lg,
  },
  previewContent: {
    flexDirection: 'row',
    flex: 1,
    alignItems: 'center',
    padding: spacing[4],
    gap: spacing[3],
    backgroundColor: colors.chipOnPrimary,
  },
  previewIcon: {
    padding: spacing[3],
    backgroundColor: colors.primary,
    borderRadius: radius.md,
  },
  previewText: {
    flex: 1,
    gap: spacing[1],
    padding: spacing[2],
    borderRadius: radius.sm,
    backgroundColor: colors.primary,
  },
  previewTag: { ...textStyles.microStrong, color: colors.logoCream },
  previewTitle: { ...textStyles.h3, color: colors.textInverse },
  previewSubtitle: { ...textStyles.micro, color: colors.textInverse },
  field: { gap: spacing[2] },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: spacing[2],
  },
  label: { ...textStyles.bodySmallStrong, color: colors.text, flexShrink: 1 },
  required: { color: colors.accent },
  optional: { ...textStyles.caption, color: colors.textSecondary },
  counter: { ...textStyles.micro, color: colors.textSecondary },
  input: {
    ...textStyles.bodySmall,
    minHeight: sizes.searchInputHeight,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
    backgroundColor: colors.surfaceMuted,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[3],
    color: colors.text,
  },
  description: { minHeight: sizes.inputHeight * 2, textAlignVertical: 'top' },
  error: { ...textStyles.caption, color: colors.danger },
  hint: { ...textStyles.caption, color: colors.textSecondary },
  imageAction: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[3],
    gap: spacing[3],
    borderWidth: sizes.borderWidth,
    borderStyle: 'dashed',
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
  },
  imageActionPlus: {
    ...textStyles.h1,
    color: colors.primary,
    paddingHorizontal: spacing[2],
  },
  linkButton: {
    minHeight: sizes.buttonHeight,
    justifyContent: 'center',
    alignSelf: 'flex-start',
  },
  link: { ...textStyles.bodySmallStrong, color: colors.primary },
  note: {
    padding: spacing[3],
    gap: spacing[1],
    backgroundColor: colors.surfacePink,
    borderRadius: radius.md,
  },
  noteText: { ...textStyles.captionStrong, color: colors.primary },
  footer: { gap: spacing[2] },
  footerHint: {
    ...textStyles.caption,
    textAlign: 'center',
    color: colors.textSecondary,
  },
});
