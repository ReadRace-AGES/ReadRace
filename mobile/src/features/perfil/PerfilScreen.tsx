import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Fragment, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AppHeader } from '@/components/AppHeader';
import { Avatar } from '@/components/avatar';
import { BookCover } from '@/components/BookCover';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { bookCover, colors, radius, sizes, spacing, textStyles } from '@/theme';
import type { Perfil } from './api';
import { usePerfil } from './usePerfil';

const numero = (valor: number) => valor.toLocaleString('pt-BR');

function IconeConquista({
  uri,
  bloqueada,
}: {
  uri: string | null;
  bloqueada: boolean;
}) {
  const [falhou, setFalhou] = useState(false);
  return uri && !falhou ? (
    <Image
      source={{ uri }}
      style={[styles.icon, bloqueada && styles.lockedImage]}
      contentFit="contain"
      onError={() => setFalhou(true)}
    />
  ) : (
    <BookIcon
      size={sizes.icon}
      color={bloqueada ? colors.textMuted : colors.primary}
    />
  );
}

export function PerfilConteudo({
  perfil,
  onPlaceholder,
}: {
  perfil: Perfil;
  onPlaceholder: () => void;
}) {
  const stats = perfil.estatisticas;
  return (
    <View style={styles.content}>
      <View style={styles.identity}>
        <View style={styles.avatarFrame}>
          <Avatar
            name={perfil.nome}
            photoUrl={perfil.avatar}
            size={sizes.avatarLarge}
          />
        </View>
        <Text style={styles.profileName}>{perfil.nome}</Text>
        <Text style={styles.profileTitle}>“{perfil.titulo}”</Text>
        <View style={styles.progressSummary}>
          <View
            style={styles.level}
            accessible
            accessibilityLabel={`Nível ${perfil.nivel}`}
          >
            <Text style={styles.levelValue}>{perfil.nivel}</Text>
          </View>
          <View style={styles.levelCopy}>
            <Text style={styles.caption}>Nível de leitura</Text>
            <Text style={styles.xp}>{numero(perfil.xpAtual)} XP</Text>
          </View>
        </View>
      </View>
      <View style={styles.social}>
        {(['Seguidores', 'Seguindo'] as const).map((label, index) => (
          <Fragment key={label}>
            {index > 0 && <View style={styles.socialDivider} />}
            <Pressable
              // Preserva o callback de estilo nativo, sem a conversão do NativeWind.
              cssInterop={false}
              accessibilityRole="button"
              onPress={onPlaceholder}
              style={({ pressed }) => [
                styles.socialButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.socialLabel}>{label}</Text>
              <Text style={styles.socialValue}>
                {numero(index === 0 ? perfil.seguidores : perfil.seguindo)}
              </Text>
            </Pressable>
          </Fragment>
        ))}
      </View>
      <Text accessibilityRole="header" style={styles.heading}>
        Estatísticas
      </Text>
      <View style={styles.grid}>
        {[
          ['Livros lidos', numero(stats.livrosLidos)],
          ['Páginas lidas', numero(stats.paginasLidas)],
          ['Sequência', `${numero(stats.sequenciaDias)} dias`],
          ['Conquistas', numero(stats.conquistas)],
        ].map(([label, value]) => (
          <View key={label} style={styles.cell}>
            <Card surfaceStyle={styles.statCard}>
              <Text style={styles.statValue}>{value}</Text>
              <Text style={styles.caption}>{label}</Text>
            </Card>
          </View>
        ))}
      </View>
      <View style={styles.sectionHeader}>
        <Text accessibilityRole="header" style={styles.heading}>
          Conquistas
        </Text>
        <Pressable
          cssInterop={false}
          accessibilityRole="button"
          accessibilityLabel="Ver mais conquistas"
          onPress={onPlaceholder}
          style={({ pressed }) => [styles.link, pressed && styles.pressed]}
        >
          <Text style={styles.linkText}>Ver mais</Text>
        </Pressable>
      </View>
      <View style={styles.grid}>
        {perfil.conquistas.map((conquista) => (
          <View
            key={conquista.id}
            style={styles.cell}
            accessible
            accessibilityLabel={`${conquista.nome}. ${conquista.descricao}. ${conquista.desbloqueada ? 'Desbloqueada' : 'Bloqueada'}`}
          >
            <Card
              style={styles.achievementCard}
              surfaceStyle={[
                styles.achievement,
                !conquista.desbloqueada && styles.locked,
              ]}
            >
              <View
                style={[
                  styles.achievementIcon,
                  !conquista.desbloqueada && styles.lockedIcon,
                ]}
              >
                <IconeConquista
                  key={conquista.icone}
                  uri={conquista.icone}
                  bloqueada={!conquista.desbloqueada}
                />
              </View>
              <Text
                style={[
                  styles.achievementName,
                  !conquista.desbloqueada && styles.muted,
                ]}
              >
                {conquista.nome}
              </Text>
              <Text style={styles.caption}>
                {conquista.desbloqueada ? 'Desbloqueada' : 'Bloqueada'}
              </Text>
            </Card>
          </View>
        ))}
      </View>
      {perfil.livrosFavoritos.length > 0 && (
        <>
          <Text accessibilityRole="header" style={styles.heading}>
            Livros favoritos
          </Text>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.favorites}
          >
            {perfil.livrosFavoritos.map((livro) => (
              <View key={livro.id} style={styles.favorite}>
                <BookCover
                  size="grid"
                  source={livro.capa}
                  accessibilityLabel={`${livro.titulo}${livro.autor ? `, ${livro.autor}` : ''}`}
                  onPress={onPlaceholder}
                />
                <Text numberOfLines={2} style={styles.favoriteTitle}>
                  {livro.titulo}
                </Text>
              </View>
            ))}
          </ScrollView>
        </>
      )}
    </View>
  );
}

export function PerfilScreen({ usuarioId }: { usuarioId: string }) {
  const { estado, recarregar } = usePerfil(usuarioId);
  const router = useRouter();
  const { showToast } = useToastContext();
  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.scroll}>
      <AppHeader
        title="Perfil"
        showBack
        onBackPress={() =>
          router.canGoBack() ? router.back() : router.replace('/buscar')
        }
      />
      {estado.situacao === 'carregando' && (
        <ActivityIndicator
          style={styles.content}
          accessibilityLabel="Carregando perfil"
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
      {estado.situacao === 'sucesso' && (
        <PerfilConteudo perfil={estado.dados} onPlaceholder={showToast} />
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  scroll: { flexGrow: 1, paddingBottom: sizes.navHeight + spacing[6] },
  content: { padding: spacing[4], gap: spacing[4] },
  identity: {
    alignItems: 'center',
    gap: spacing[2],
    padding: spacing[6],
    backgroundColor: colors.surfacePink,
    borderRadius: radius.xl,
  },
  avatarFrame: {
    padding: spacing[1],
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
  },
  profileName: {
    ...textStyles.h1,
    color: colors.primary,
    textAlign: 'center',
    marginTop: spacing[1],
  },
  profileTitle: {
    ...textStyles.bodySmall,
    color: colors.primarySoft,
    textAlign: 'center',
  },
  progressSummary: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    marginTop: spacing[3],
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[4],
    backgroundColor: colors.surface,
    borderRadius: radius.pill,
  },
  levelCopy: { flexShrink: 1, gap: spacing[1] },
  levelValue: { ...textStyles.h3, color: colors.primary },
  xp: { ...textStyles.bodyStrong, color: colors.primary },
  caption: { ...textStyles.caption, color: colors.textSecondary },
  name: { ...textStyles.h3, color: colors.text },
  heading: { ...textStyles.h3, color: colors.text, marginTop: spacing[2] },
  body: { ...textStyles.bodySmall, color: colors.text },
  level: {
    borderRadius: radius.pill,
    borderWidth: sizes.borderWidth,
    borderColor: colors.primary,
    width: sizes.avatarLarge,
    height: sizes.avatarLarge,
    alignItems: 'center',
    justifyContent: 'center',
  },
  social: {
    flexDirection: 'row',
    alignItems: 'stretch',
    borderWidth: sizes.borderWidth,
    borderColor: colors.textSecondary,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    overflow: 'hidden',
  },
  socialDivider: {
    width: sizes.borderWidth,
    backgroundColor: colors.textSecondary,
    marginVertical: spacing[2],
  },
  socialLabel: { ...textStyles.micro, color: colors.textSecondary },
  socialValue: { ...textStyles.bodySmall, color: colors.text },
  socialButton: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    minWidth: 0,
    padding: spacing[2],
    minHeight: sizes.buttonHeight,
  },
  grid: { flexDirection: 'row', flexWrap: 'wrap', rowGap: spacing[3] },
  cell: { width: '50%', paddingHorizontal: spacing[1] },
  statCard: {
    backgroundColor: colors.surface,
    gap: spacing[1],
    borderWidth: sizes.borderWidth,
    borderColor: colors.border,
  },
  statValue: { ...textStyles.h1, color: colors.primary },
  achievementCard: { flexGrow: 1 },
  achievement: {
    flexGrow: 1,
    alignItems: 'center',
    gap: spacing[2],
    backgroundColor: colors.surface,
    borderWidth: sizes.borderWidth,
    borderColor: colors.border,
  },
  achievementIcon: {
    padding: spacing[3],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  achievementName: {
    ...textStyles.bodySmallStrong,
    flexGrow: 1,
    color: colors.text,
    textAlign: 'center',
  },
  lockedIcon: { backgroundColor: colors.surfaceAlt },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  link: {
    borderRadius: radius.pill,
    padding: spacing[2],
    minHeight: sizes.buttonHeight,
    justifyContent: 'center',
  },
  linkText: { ...textStyles.bodySmallStrong, color: colors.accent },
  locked: { backgroundColor: colors.surfaceDisabled },
  muted: { color: colors.textMuted },
  icon: { width: sizes.icon, height: sizes.icon },
  // As imagens da API podem ter fundo opaco; tintColor apagaria seu conteúdo.
  lockedImage: { opacity: 0.5 },
  favorites: { gap: spacing[4], paddingBottom: spacing[2] },
  favorite: { width: bookCover.grid.width, gap: spacing[2] },
  favoriteTitle: { ...textStyles.captionStrong, color: colors.text },
  pressed: { backgroundColor: colors.surfacePink },
});
