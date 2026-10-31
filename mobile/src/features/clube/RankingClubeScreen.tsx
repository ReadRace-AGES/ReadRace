import { useRouter, type Href } from 'expo-router';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  StyleSheet,
  View,
} from 'react-native';

import { AppHeader } from '@/components/AppHeader';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { RankingRow } from '@/components/RankingRow';
import { colors, spacing } from '@/theme';

import { FaixaQuadriculada, RankingCard } from './RankingCard';
import { useRankingClube } from './useRankingClube';

const COPY = {
  titulo: 'Ranking',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar',
} as const;

function rotaDoPerfilUsuario(usuarioId: string): Href {
  return {
    pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
    params: { usuarioId },
  } as unknown as Href;
}

export function RankingClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const { ranking, recarregar, carregarMais } = useRankingClube(clubeId);

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }

    router.replace('/feed');
  }

  // Task #202: abre o perfil do participante selecionado.
  function abrirPerfilDoRanking(usuarioId: string) {
    router.push(rotaDoPerfilUsuario(usuarioId));
  }

  if (ranking.situacao === 'erro') {
    return (
      <View style={styles.tela}>
        <AppHeader
          variant="surface"
          titleAlign="center"
          showBack
          onBackPress={voltar}
          title={COPY.titulo}
        />
        <EmptyState
          icon={BookIcon}
          message={ranking.mensagem}
          action={
            <View style={styles.acoesDoErro}>
              <PrimaryButton
                label={COPY.tentarDeNovo}
                onPress={recarregar}
                variant="outline"
              />

              <PrimaryButton
                label={COPY.voltar}
                onPress={voltar}
                variant="outline"
              />
            </View>
          }
        />
      </View>
    );
  }

  const dados = ranking.situacao === 'sucesso' ? ranking.dados : null;

  return (
    <View style={styles.tela}>
      <AppHeader
        variant="surface"
        titleAlign="center"
        showBack
        onBackPress={voltar}
        title={COPY.titulo}
        subtitle={dados?.clube.nome}
      />

      {dados && dados.ranking.length > 0 ? (
        <Card style={styles.card} surfaceStyle={styles.superficie}>
          <FaixaQuadriculada />

          <FlatList
            data={dados.ranking}
            keyExtractor={(linha) => linha.usuario.id}
            renderItem={({ item, index }) => (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={`Ver perfil de ${item.usuario.nome}`}
                onPress={() => abrirPerfilDoRanking(item.usuario.id)}
              >
                <RankingRow
                  position={item.posicao}
                  rowIndex={index}
                  name={item.usuario.nome}
                  points={item.pontos}
                  photoUrl={item.usuario.avatarUrl}
                />
              </Pressable>
            )}
            onEndReached={carregarMais}
            onEndReachedThreshold={0.5}
            ListFooterComponent={
              ranking.situacao === 'sucesso' && ranking.carregandoMais ? (
                <ActivityIndicator
                  color={colors.primary}
                  style={styles.carregandoMais}
                />
              ) : null
            }
          />
        </Card>
      ) : (
        <View style={styles.conteudo}>
          <RankingCard
            ranking={dados?.ranking ?? null}
            onLinhaPress={abrirPerfilDoRanking}
          />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  tela: {
    flex: 1,
    backgroundColor: colors.surface,
  },

  conteudo: {
    padding: spacing[6],
  },

  card: {
    flex: 1,
    margin: spacing[6],
  },

  superficie: {
    flex: 1,
    padding: spacing[0],
  },

  carregandoMais: {
    padding: spacing[4],
  },

  acoesDoErro: {
    alignSelf: 'stretch',
    gap: spacing[2],
  },
});

export default RankingClubeScreen;
