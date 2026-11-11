import { useCallback } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useRouter, type Href } from 'expo-router';

import { AppHeader } from '@/components/AppHeader';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { LinhaUsuario } from '@/components/LinhaUsuario';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { formatarData } from '@/features/conquistas/model';
import { colors, spacing, textStyles } from '@/theme';

import { useSolicitacoesClube } from './useSolicitacoesClube';

export function SolicitacoesClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const { showErrorToast, showToastAfterNavigation } = useToastContext();
  const encerrarAcesso = useCallback(
    (mensagem: string) => {
      showToastAfterNavigation(mensagem);
      router.replace(`/clube/${clubeId}`);
    },
    [clubeId, router, showToastAfterNavigation]
  );
  const { carga, acoes, recarregar, responder } = useSolicitacoesClube(
    clubeId,
    { onFeedback: showErrorToast, onSemAcesso: encerrarAcesso }
  );

  const mensagemErro =
    carga.situacao === 'erro'
      ? carga.mensagem
      : carga.situacao === 'sucesso'
        ? carga.erroAtualizacao
        : undefined;
  return (
    <View style={styles.tela}>
      <AppHeader
        variant="surface"
        title="Solicitações"
        titleAlign="center"
        showBack
        onBackPress={() =>
          router.canGoBack()
            ? router.back()
            : router.replace(`/clube/${clubeId}`)
        }
      />
      {carga.situacao === 'carregando' && (
        <View style={styles.centralizado}>
          <ActivityIndicator
            accessibilityLabel="Carregando solicitações"
            color={colors.primary}
          />
        </View>
      )}
      {mensagemErro && (
        <View style={styles.erroContainer}>
          <Text accessibilityRole="alert" style={styles.erro}>
            {mensagemErro}
          </Text>
          <PrimaryButton
            label="Tentar de novo"
            variant="outline"
            onPress={() => recarregar(carga.situacao === 'sucesso')}
          />
        </View>
      )}
      {carga.situacao === 'sucesso' &&
        (carga.solicitacoes.length === 0 ? (
          <EmptyState
            icon={BookIcon}
            message="Nenhum pedido para entrar no clube."
          />
        ) : (
          <ScrollView contentContainerStyle={styles.lista}>
            {carga.solicitacoes.map((pedido) => (
              <Card key={pedido.id} surfaceStyle={styles.card}>
                <LinhaUsuario
                  nome={pedido.usuario.nome}
                  nomeUsuario={pedido.usuario.nomeUsuario.replace(/^@+/, '')}
                  avatarUrl={pedido.usuario.avatarUrl}
                  titulo={`Pedido em ${formatarData(pedido.criadaEm)}`}
                  onPress={() =>
                    router.push(
                      `/(tabs)/(feed)/feed-usuario/${encodeURIComponent(pedido.usuario.id)}` as Href
                    )
                  }
                />
                <View style={styles.acoes}>
                  <View style={styles.botao}>
                    <PrimaryButton
                      label="Aceitar"
                      disabled={!!acoes[pedido.id]}
                      loading={acoes[pedido.id] === 'aceitar'}
                      onPress={() => {
                        void responder(pedido.id, 'aceitar');
                      }}
                    />
                  </View>
                  <View style={styles.botao}>
                    <PrimaryButton
                      label="Recusar"
                      variant="outline"
                      disabled={!!acoes[pedido.id]}
                      loading={acoes[pedido.id] === 'recusar'}
                      onPress={() => {
                        void responder(pedido.id, 'recusar');
                      }}
                    />
                  </View>
                </View>
              </Card>
            ))}
          </ScrollView>
        ))}
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  centralizado: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  lista: { padding: spacing[6], paddingBottom: spacing[10], gap: spacing[4] },
  card: { padding: spacing[0] },
  acoes: { flexDirection: 'row', padding: spacing[4], gap: spacing[3] },
  botao: { flex: 1 },
  erroContainer: { padding: spacing[6], gap: spacing[4] },
  erro: { ...textStyles.body, color: colors.danger, textAlign: 'center' },
});
