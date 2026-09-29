import { useRouter } from 'expo-router';
import type { ComponentType } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { LockIcon } from '@/components/icons/LockIcon';
import { MedalIcon } from '@/components/icons/MedalIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import {
  colors,
  radius,
  shadows,
  sizes,
  spacing,
  textStyles,
  typography,
} from '@/theme';
import type { Conquista } from './api';
import { IconeConquista } from './IconeConquista';
import { formatarData, separarConquistas } from './model';
import { useMinhasConquistas } from './useMinhasConquistas';

const TAMANHO_ICONE = spacing[10] + spacing[6];

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
      <View style={[styles.card, !bloqueada && styles.cardEarned]}>
        <View style={[styles.icon, bloqueada && styles.iconLocked]}>
          <IconeConquista
            key={conquista.icone}
            uri={conquista.icone}
            bloqueada={bloqueada}
            size={TAMANHO_ICONE}
          />
        </View>
        <View style={styles.copy}>
          <Text
            numberOfLines={1}
            style={[styles.name, bloqueada && styles.nameLocked]}
          >
            {conquista.nome}
          </Text>
          {!bloqueada && (
            <Text numberOfLines={2} style={styles.description}>
              {conquista.descricao}
            </Text>
          )}
          {linha ? (
            <Text
              numberOfLines={2}
              style={bloqueada ? styles.description : styles.date}
            >
              {linha}
            </Text>
          ) : null}
        </View>
      </View>
    </View>
  );
}

function Secao({
  titulo,
  icone: Icone,
  conquistas,
  bloqueadas = false,
}: {
  titulo: string;
  icone: ComponentType<{ size?: number; color?: string }>;
  conquistas: Conquista[];
  bloqueadas?: boolean;
}) {
  if (conquistas.length === 0) return null;
  const cor = bloqueadas ? colors.textSecondary : colors.primarySoft;
  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <Icone size={sizes.icon} color={cor} />
        <Text
          accessibilityRole="header"
          style={[styles.heading, bloqueadas && styles.headingLocked]}
        >
          {titulo}
        </Text>
      </View>
      <View style={[styles.cards, bloqueadas && styles.cardsLocked]}>
        {conquistas.map((conquista) => (
          <CardConquista key={conquista.id} conquista={conquista} />
        ))}
      </View>
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
      <View style={styles.header}>
        <AppHeader
          compact
          titleAlign="center"
          title="Minhas Conquistas"
          subtitle="Visualize seu progresso e medalhas"
          showBack
          onBackPress={() =>
            router.canGoBack() ? router.back() : router.replace('/perfil')
          }
        />
      </View>
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
            bloqueadas
          />
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  scroll: { flexGrow: 1, paddingBottom: sizes.navHeight + spacing[6] },
  header: {
    borderBottomLeftRadius: radius.xl,
    borderBottomRightRadius: radius.xl,
    overflow: 'hidden',
  },
  content: {
    paddingTop: spacing[6],
    paddingHorizontal: spacing[5],
    gap: spacing[10],
  },
  section: { gap: spacing[4] },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  heading: {
    ...textStyles.h2,
    fontFamily: typography.fontFamily.semibold,
    color: colors.text,
  },
  headingLocked: { color: colors.textSecondary },
  cards: { gap: spacing[4] },
  cardsLocked: { opacity: 0.7 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[4],
    padding: spacing[4],
    borderRadius: radius.md,
    borderWidth: sizes.borderWidth,
    borderColor: colors.surfacePinkStrong,
    backgroundColor: colors.surfacePink,
  },
  cardEarned: shadows.input,
  icon: {
    width: TAMANHO_ICONE,
    height: TAMANHO_ICONE,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.sm,
    backgroundColor: colors.surfacePinkStrong,
    overflow: 'hidden',
  },
  iconLocked: { backgroundColor: colors.surfaceAlt },
  copy: { flex: 1, minWidth: 0, gap: spacing[1] },
  name: {
    ...textStyles.bodySmall,
    fontFamily: typography.fontFamily.semibold,
    color: colors.primary,
  },
  nameLocked: { color: colors.text },
  description: { ...textStyles.bodySmall, color: colors.textSecondary },
  date: {
    ...textStyles.caption,
    paddingTop: spacing[1],
    color: colors.primarySoft,
  },
});
