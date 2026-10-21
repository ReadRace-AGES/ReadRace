import { StyleSheet } from 'react-native';

import { colors, radius, sizes, spacing, textStyles } from '@/theme';

export const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  rolagem: { flexGrow: 1, paddingBottom: spacing[10] },
  hero: { height: 248, overflow: 'hidden', position: 'relative' },
  heroImage: { ...StyleSheet.absoluteFill },
  heroPlaceholder: {
    ...StyleSheet.absoluteFill,
    backgroundColor: colors.surfaceAlt,
  },
  heroShade: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(0, 0, 0, 0.18)',
  },
  back: {
    position: 'absolute',
    left: spacing[6],
    top: spacing[4],
    padding: spacing[2],
  },
  conteudo: {
    marginTop: -24,
    marginHorizontal: spacing[4],
    padding: spacing[5],
    paddingTop: spacing[4],
    gap: spacing[4],
    flexGrow: 1,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    backgroundColor: colors.surface,
  },
  carregando: { flexGrow: 1, justifyContent: 'center', alignItems: 'center' },
  tituloLinha: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[3],
  },
  titulo: { ...textStyles.h1, flex: 1, color: colors.quizErradoTexto },
  seguir: { ...textStyles.caption, color: colors.accent },
  resumo: {
    minHeight: 54,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-evenly',
    borderWidth: sizes.borderWidth,
    borderColor: colors.text,
    borderRadius: radius.md,
  },
  metrica: { alignItems: 'center', flex: 1 },
  seguidores: { ...textStyles.bodySmallStrong, color: colors.text },
  rotulo: { ...textStyles.micro, color: colors.textSecondary },
  postagens: { ...textStyles.bodyStrong, color: colors.text },
  abas: { flexDirection: 'row', gap: spacing[6] },
  abaAtiva: { ...textStyles.caption, color: colors.accent, textDecorationLine: 'underline' },
  aba: { ...textStyles.caption, color: colors.accent, textDecorationLine: 'underline' },
  post: { gap: spacing[1] },
});
