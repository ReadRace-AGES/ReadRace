import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { colors, spacing } from '@/theme';

import { RankingCard } from './RankingCard';
import { useRankingClube } from './useRankingClube';

const COPY = {
  titulo: 'Ranking',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar',
} as const;

export function RankingClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const { showToast } = useToastContext();
  const { ranking, recarregar } = useRankingClube(clubeId);

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace('/feed');
  }

  if (ranking.situacao === 'erro') {
    return (
      <View style={styles.tela}>
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
      <ScrollView contentContainerStyle={styles.rolagem}>
        <AppHeader
          variant="surface"
          titleAlign="center"
          showBack
          onBackPress={voltar}
          title={COPY.titulo}
          subtitle={dados?.clube.nome}
        />

        <View style={styles.conteudo}>
          <RankingCard
            ranking={dados?.ranking ?? null}
            onLinhaPress={showToast}
          />
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  rolagem: { flexGrow: 1, paddingBottom: spacing[10] },
  conteudo: { padding: spacing[6] },
  acoesDoErro: { alignSelf: 'stretch', gap: spacing[2] },
});

export default RankingClubeScreen;
