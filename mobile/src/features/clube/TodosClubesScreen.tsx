import { useCallback, useState } from 'react';
import { ActivityIndicator, Keyboard, ScrollView, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ClubeCard } from '@/features/feed/ClubeCard';
import { CriarGrupoSheet } from '@/features/feed/CriarGrupoSheet';
import { colors, spacing } from '@/theme';

import { EntradaClubeButton } from './EntradaClubeButton';
import { useTodosClubes } from './useTodosClubes';

export function TodosClubesScreen() {
  const router = useRouter();
  const { estado, recarregar, pedidoEnviado } = useTodosClubes();
  const [criando, setCriando] = useState(false);
  const fechar = useCallback(() => {
    Keyboard.dismiss();
    setCriando(false);
  }, []);
  useFocusEffect(useCallback(() => () => fechar(), [fechar]));
  return (
    <View className="flex-1 bg-surface">
      <AppHeader
        title="Todos os clubes"
        variant="surface"
        showBack
        onBackPress={() =>
          router.canGoBack() ? router.back() : router.replace('/feed')
        }
      />
      {estado.situacao === 'carregando' && (
        <View
          className="flex-1 items-center justify-center"
          accessibilityLabel="Carregando clubes"
        >
          <ActivityIndicator color={colors.primary} />
        </View>
      )}
      {estado.situacao === 'erro' && (
        <EmptyState
          icon={BookIcon}
          message={estado.mensagem}
          messageStyle={{ color: colors.danger }}
          action={
            <PrimaryButton
              label="Tentar de novo"
              onPress={() => recarregar()}
              variant="outline"
            />
          }
        />
      )}
      {estado.situacao === 'sucesso' &&
        (estado.clubes.length === 0 ? (
          <EmptyState
            icon={BookIcon}
            message="Nenhum clube por aqui ainda."
            action={
              <PrimaryButton
                label="Criar Grupo"
                onPress={() => setCriando(true)}
              />
            }
          />
        ) : (
          <ScrollView
            contentContainerStyle={{
              padding: spacing[6],
              gap: spacing[4],
              paddingBottom: spacing[10],
            }}
          >
            {estado.clubes.map((clube) => (
              <View key={clube.id} style={{ gap: spacing[2] }}>
                <ClubeCard
                  clube={{
                    id: clube.id,
                    nome: clube.nome,
                    capaUrl: clube.capaUrl,
                    livroAtual: clube.livro,
                    totalMembros: clube.membros,
                  }}
                  onPress={() => router.push(`/clube/${clube.id}`)}
                />
                <EntradaClubeButton
                  clubeId={clube.id}
                  situacao={clube.situacao}
                  onPedido={() => pedidoEnviado(clube.id)}
                  onReconciliar={() => recarregar(true)}
                />
              </View>
            ))}
          </ScrollView>
        ))}
      <CriarGrupoSheet
        visible={criando}
        onClose={fechar}
        onSuccess={() => recarregar(true)}
      />
    </View>
  );
}
