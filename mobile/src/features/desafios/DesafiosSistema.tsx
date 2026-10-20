import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/Card';
import { ClockIcon } from '@/components/icons/ClockIcon';
import { TargetIcon } from '@/components/icons/TargetIcon';
import { TrophyIcon } from '@/components/icons/TrophyIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ProgressBar } from '@/components/ProgressBar';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import type { DesafioSistema } from './sistemaApi';
import { useDesafiosSistema } from './useDesafiosSistema';

function XpBadge({ xp }: { xp: number }) {
  return <Text style={styles.badge}>+ {xp} XP</Text>;
}

function Barra({ desafio }: { desafio: DesafioSistema }) {
  // O backend mede o progresso; aqui apenas convertemos unidades em largura.
  const percentual = (desafio.progresso / desafio.meta) * 100;
  return <ProgressBar progress={percentual} showPercent={false} />;
}

export function DesafioMensalCard({ desafio }: { desafio: DesafioSistema }) {
  return (
    <Card>
      <View style={styles.content}>
        <View style={styles.row}>
          <View style={styles.copy}>
            <Text style={styles.title}>{desafio.titulo}</Text>
            <Text style={styles.description}>{desafio.descricao}</Text>
          </View>
          <TrophyIcon size={sizes.avatar} color={colors.primary} />
        </View>
        <Text style={styles.progress}>
          {desafio.progresso}/{desafio.meta} livros
        </Text>
        <Barra desafio={desafio} />
        <View style={styles.divider} />
        <View style={styles.footer}>
          <View style={styles.deadline}>
            {!desafio.cumprido && (
              <ClockIcon size={sizes.iconSmall} color={colors.textSecondary} />
            )}
            <Text style={styles.deadlineText}>
              {desafio.cumprido
                ? 'Desafio cumprido!'
                : `${desafio.diasRestantes} dias restantes`}
            </Text>
          </View>
          <XpBadge xp={desafio.xp} />
        </View>
      </View>
    </Card>
  );
}

export function DesafioSemanalCard({ desafio }: { desafio: DesafioSistema }) {
  return (
    <Card>
      <View style={styles.content}>
        <View style={styles.row}>
          <View style={styles.target}>
            <TargetIcon size={sizes.icon} color={colors.primary} />
          </View>
          <Text style={[styles.description, styles.copy]}>
            {desafio.descricao}
          </Text>
        </View>
        <View style={styles.footer}>
          <Text style={styles.progress}>
            {desafio.progresso}/{desafio.meta} páginas
          </Text>
          <XpBadge xp={desafio.xp} />
        </View>
        <Barra desafio={desafio} />
        {desafio.cumprido && (
          <Text style={styles.completed}>Desafio cumprido!</Text>
        )}
      </View>
    </Card>
  );
}

export function DesafiosSistema() {
  const { estado, recarregar } = useDesafiosSistema();

  return (
    <>
      {(['mensal', 'semanal'] as const).map((tipo) => (
        <View key={tipo} style={styles.section}>
          <Text style={styles.sectionTitle}>
            {tipo === 'mensal' ? 'Desafio ativo' : 'Desafio semanal'}
          </Text>
          {estado.situacao === 'carregando' && (
            <Card>
              <View style={styles.loading}>
                <ActivityIndicator color={colors.primary} />
                <Text style={styles.description}>Carregando desafio...</Text>
              </View>
            </Card>
          )}
          {estado.situacao === 'erro' && (
            <Card>
              <View style={styles.content}>
                <Text accessibilityRole="alert" style={styles.error}>
                  {estado.mensagem}
                </Text>
                <PrimaryButton
                  label="Tentar novamente"
                  variant="outline"
                  onPress={recarregar}
                />
              </View>
            </Card>
          )}
          {estado.situacao === 'sucesso' &&
            (tipo === 'mensal' ? (
              <DesafioMensalCard desafio={estado.desafios.mensal} />
            ) : (
              <DesafioSemanalCard desafio={estado.desafios.semanal} />
            ))}
        </View>
      ))}
    </>
  );
}

const styles = StyleSheet.create({
  section: { gap: spacing[4] },
  sectionTitle: { ...textStyles.h3, color: colors.text },
  content: { gap: spacing[3] },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing[3] },
  copy: { flex: 1, minWidth: 0, gap: spacing[1] },
  title: { ...textStyles.bodyStrong, color: colors.text },
  description: { ...textStyles.bodySmall, color: colors.textSecondary },
  progress: {
    ...textStyles.bodySmallStrong,
    color: colors.text,
    flexShrink: 1,
  },
  divider: { height: sizes.borderWidth, backgroundColor: colors.borderStrong },
  footer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[2],
  },
  deadline: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
    flexShrink: 1,
  },
  deadlineText: {
    ...textStyles.caption,
    color: colors.textSecondary,
    flexShrink: 1,
  },
  badge: {
    ...textStyles.captionStrong,
    color: colors.primary,
    backgroundColor: colors.surfacePink,
    borderRadius: radius.pill,
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[1],
    flexShrink: 1,
  },
  target: {
    padding: spacing[3],
    borderRadius: radius.md,
    backgroundColor: colors.surfacePink,
  },
  completed: { ...textStyles.bodySmallStrong, color: colors.success },
  loading: {
    gap: spacing[2],
    alignItems: 'center',
    paddingVertical: spacing[4],
  },
  error: { ...textStyles.bodySmall, color: colors.danger },
});
