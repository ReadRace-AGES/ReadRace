import { useRouter } from 'expo-router';
import type { ComponentType } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppHeader } from '@/components/AppHeader';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { LockIcon } from '@/components/icons/LockIcon';
import { MedalIcon } from '@/components/icons/MedalIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';
import type { Conquista } from './api';
import { IconeConquista } from './IconeConquista';
import { formatarData, separarConquistas } from './model';
import { useMinhasConquistas } from './useMinhasConquistas';

function CardConquista({ conquista }: { conquista: Conquista }) {
  const bloqueada = !conquista.desbloqueada;
  const linha = bloqueada
    ? `Para desbloquear: ${conquista.descricao}`
    : conquista.data && `Alcançado em ${formatarData(conquista.data)}`;
  return (
    <View
      accessible
      accessibilityLabel={[
        conquista.nome,
        bloqueada ? 'Bloqueada' : conquista.descricao,
        linha,
      ]
        .filter(Boolean)
        .join('. ')}
    >
      <Card surfaceStyle={[styles.card, bloqueada && styles.locked]}>
        <View style={[styles.icon, bloqueada && styles.lockedIcon]}>
          <IconeConquista
            key={conquista.icone}
            uri={conquista.icone}
            bloqueada={bloqueada}
          />
        </View>
        <View style={styles.copy}>
          <Text
            numberOfLines={1}
            style={[styles.name, bloqueada && styles.muted]}
          >
            {conquista.nome}
          </Text>
          {!bloqueada && (
            <Text numberOfLines={1} style={styles.description}>
              {conquista.descricao}
            </Text>
          )}
          {linha ? (
            <Text
              numberOfLines={1}
              style={[styles.detail, bloqueada && styles.muted]}
            >
              {linha}
            </Text>
          ) : null}
        </View>
      </Card>
    </View>
  );
}

function Secao({
  titulo,
  icone: Icone,
  conquistas,
}: {
  titulo: string;
  icone: ComponentType<{ size?: number; color?: string }>;
  conquistas: Conquista[];
}) {
  if (conquistas.length === 0) return null;
  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <Icone size={sizes.icon} color={colors.primary} />
        <Text accessibilityRole="header" style={styles.heading}>
          {titulo}
        </Text>
      </View>
      {conquistas.map((conquista) => (
        <CardConquista key={conquista.id} conquista={conquista} />
      ))}
    </View>
  );
}

export function MinhasConquistasScreen() {
  const { estado, recarregar } = useMinhasConquistas();
  const router = useRouter();
  const secoes =
    estado.situacao === 'sucesso' ? separarConquistas(estado.dados) : null;
  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.scroll}>
      <AppHeader
        title="Minhas Conquistas"
        subtitle="Visualize seu progresso e medalhas"
        showBack
        onBackPress={() =>
          router.canGoBack() ? router.back() : router.replace('/perfil')
        }
      />
      {estado.situacao === 'carregando' && (
        <ActivityIndicator
          style={styles.content}
          accessibilityLabel="Carregando conquistas"
          color={colors.primary}
        />
      )}
      {estado.situacao === 'erro' && (
        <EmptyState
          icon={BookIcon}
          message={estado.mensagem}
          action={
            <PrimaryButton label="Tentar novamente" onPress={recarregar} />
          }
        />
      )}
      {secoes && (
        <View style={styles.content}>
          <Secao
            titulo="Conquistas Desbloqueadas"
            icone={MedalIcon}
            conquistas={secoes.desbloqueadas}
          />
          <Secao
            titulo="Próximas Conquistas"
            icone={LockIcon}
            conquistas={secoes.proximas}
          />
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  scroll: { flexGrow: 1, paddingBottom: sizes.navHeight + spacing[6] },
  content: { padding: spacing[4], gap: spacing[6] },
  section: { gap: spacing[3] },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing[2] },
  heading: { ...textStyles.h3, color: colors.text },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    backgroundColor: colors.surface,
    borderWidth: sizes.borderWidth,
    borderColor: colors.border,
  },
  icon: {
    padding: spacing[3],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  copy: { flex: 1, minWidth: 0, gap: spacing[1] },
  name: { ...textStyles.bodyStrong, color: colors.text },
  description: { ...textStyles.bodySmall, color: colors.text },
  detail: { ...textStyles.caption, color: colors.textSecondary },
  locked: { backgroundColor: colors.surfaceDisabled },
  lockedIcon: { backgroundColor: colors.surfaceAlt },
  muted: { color: colors.textMuted },
});
