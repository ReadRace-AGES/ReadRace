import { useCallback, useRef } from 'react';
import { ActivityIndicator, FlatList, View } from 'react-native';
import {
  useFocusEffect,
  useLocalSearchParams,
  useRouter,
  type Href,
} from 'expo-router';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { LinhaUsuario } from '@/components/LinhaUsuario';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing } from '@/theme';
import { useListaSeguidores } from './useListaSeguidores';

export function ListaSeguidoresScreen({
  origem,
}: {
  origem: 'perfil' | 'busca';
}) {
  const { usuarioId, tipo } = useLocalSearchParams<{
    usuarioId: string;
    tipo?: string;
  }>();
  const lista = tipo === 'seguindo' ? 'seguindo' : 'seguidores';
  const { carga, recarregar } = useListaSeguidores(usuarioId, lista);
  const router = useRouter();
  const primeiroFoco = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (primeiroFoco.current) {
        primeiroFoco.current = false;
        return;
      }
      recarregar();
    }, [recarregar])
  );
  return (
    <View className="flex-1 bg-surface">
      <AppHeader
        compact
        title={lista === 'seguidores' ? 'Seguidores' : 'Seguindo'}
        subtitle={carga.situacao === 'sucesso' ? carga.nome : undefined}
        showBack
        onBackPress={() =>
          router.canGoBack()
            ? router.back()
            : router.replace(origem === 'perfil' ? '/perfil' : '/buscar')
        }
      />
      {carga.situacao === 'carregando' && (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator
            accessibilityLabel="Carregando lista"
            color={colors.primary}
          />
        </View>
      )}
      {carga.situacao === 'erro' && (
        <EmptyState
          icon={SearchIcon}
          message={carga.mensagem}
          action={<PrimaryButton label="Tentar de novo" onPress={recarregar} />}
        />
      )}
      {carga.situacao === 'sucesso' && (
        <FlatList
          data={carga.usuarios}
          keyExtractor={(item) => item.id}
          contentContainerStyle={{ flexGrow: 1, paddingBottom: spacing[6] }}
          ListEmptyComponent={
            <EmptyState
              icon={SearchIcon}
              message={
                lista === 'seguidores'
                  ? 'Ninguém segue este perfil ainda.'
                  : 'Este perfil ainda não segue ninguém.'
              }
            />
          }
          renderItem={({ item }) => (
            <LinhaUsuario
              {...item}
              onPress={() => {
                if (item.id === carga.eu) {
                  router.navigate('/(tabs)/(perfil)/perfil');
                  return;
                }
                router.push(
                  (origem === 'perfil'
                    ? `/(tabs)/(perfil)/perfil-usuario/${encodeURIComponent(item.id)}`
                    : `/(tabs)/(busca)/usuario/${encodeURIComponent(item.id)}`) as Href
                );
              }}
            />
          )}
        />
      )}
    </View>
  );
}
