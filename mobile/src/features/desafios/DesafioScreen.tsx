import { useCallback, useRef, useState } from 'react';
import { useFocusEffect, useRouter } from 'expo-router';

import { ActivityIndicator, ScrollView, Text, View } from 'react-native';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { Avatar } from '@/components/avatar';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { UsersIcon } from '@/components/icons/UsersIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing, textStyles } from '@/theme';

import { listarDesafios, type Desafio } from './api';

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'sucesso'; desafios: Desafio[] }
  | { situacao: 'erro'; mensagem: string };

function DesafioCard({ desafio }: { desafio: Desafio }) {
  const voceEstaNaFrente = desafio.progresso.voce > desafio.progresso.oponente;

  const oponenteEstaNaFrente =
    desafio.progresso.oponente > desafio.progresso.voce;

  return (
    <Card>
      <View style={{ gap: spacing[3] }}>
        <View className="flex-row items-center">
          <Avatar
            name={desafio.oponente.username}
            photoUrl={desafio.oponente.avatarUrl}
          />

          <View className="min-w-0 flex-1" style={{ marginLeft: spacing[3] }}>
            <Text
              numberOfLines={1}
              style={[textStyles.bodyStrong, { color: colors.text }]}
            >
              {desafio.oponente.username}
            </Text>

            <Text
              numberOfLines={2}
              style={[textStyles.caption, { color: colors.textSecondary }]}
            >
              {desafio.descricao}
            </Text>
          </View>

          <View
            style={{
              borderWidth: 1,
              borderColor: colors.borderStrong,
              borderRadius: 9999,
              paddingHorizontal: spacing[2],
              paddingVertical: spacing[1],
            }}
          >
            <Text style={[textStyles.micro, { color: colors.text }]}>
              {desafio.diasRestantes} dias
            </Text>
          </View>
        </View>

        <View
          style={{
            height: 1,
            backgroundColor: colors.border,
          }}
        />

        <View className="flex-row">
          <View className="flex-1 items-center">
            <Text
              style={[
                textStyles.h2,
                {
                  color: voceEstaNaFrente ? colors.primary : colors.text,
                },
              ]}
            >
              {desafio.progresso.voce}
            </Text>

            <Text style={[textStyles.caption, { color: colors.textSecondary }]}>
              Você
            </Text>
          </View>

          <View className="flex-1 items-center">
            <Text
              style={[
                textStyles.h2,
                {
                  color: oponenteEstaNaFrente ? colors.primary : colors.text,
                },
              ]}
            >
              {desafio.progresso.oponente}
            </Text>

            <Text
              numberOfLines={1}
              style={[textStyles.caption, { color: colors.textSecondary }]}
            >
              {desafio.oponente.username}
            </Text>
          </View>
        </View>
      </View>
    </Card>
  );
}

export function DesafiosScreen() {
  const router = useRouter();
  const requisicao = useRef<AbortController | null>(null);

  const [estado, setEstado] = useState<Estado>({
    situacao: 'carregando',
  });

  const carregarDesafios = useCallback(async () => {
    requisicao.current?.abort();
    const controller = new AbortController();
    requisicao.current = controller;
    setEstado({ situacao: 'carregando' });

    try {
      const resposta = await listarDesafios(controller.signal);

      if (controller.signal.aborted) return;

      setEstado({
        situacao: 'sucesso',
        desafios: resposta.desafios,
      });
    } catch (erro: unknown) {
      if (controller.signal.aborted) return;

      setEstado({
        situacao: 'erro',
        mensagem:
          erro instanceof ApiError
            ? erro.message
            : erro instanceof Error
              ? erro.message
              : '',
      });
    } finally {
      if (requisicao.current === controller) requisicao.current = null;
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      carregarDesafios();
      return () => {
        requisicao.current?.abort();
        requisicao.current = null;
      };
    }, [carregarDesafios])
  );

  function desafiarAmigo() {
    router.push('/desafiar-amigo');
  }

  const listaVazia =
    estado.situacao === 'sucesso' && estado.desafios.length === 0;

  return (
    <View className="flex-1 bg-surface">
      <AppHeader
        title="Desafios"
        subtitle="Supere seus limites e ganhe recompensas"
      />

      <ScrollView
        className="flex-1"
        contentContainerStyle={{
          padding: spacing[6],
          gap: spacing[4],
        }}
      >
        <Text style={[textStyles.h3, { color: colors.text }]}>
          Desafio com amigos
        </Text>

        {estado.situacao === 'carregando' && (
          <View
            className="items-center justify-center"
            style={{
              paddingVertical: spacing[8],
            }}
          >
            <ActivityIndicator color={colors.primary} />
          </View>
        )}

        {estado.situacao === 'erro' && (
          <View style={{ gap: spacing[4] }}>
            <Text
              style={[
                textStyles.body,
                {
                  color: colors.textSecondary,
                  textAlign: 'center',
                },
              ]}
            >
              {estado.mensagem}
            </Text>

            <PrimaryButton
              label="Tentar de novo"
              variant="outline"
              onPress={carregarDesafios}
            />
          </View>
        )}

        {listaVazia && (
          <EmptyState
            icon={UsersIcon}
            message="Você ainda não possui desafios com amigos."
          />
        )}

        {estado.situacao === 'sucesso' &&
          estado.desafios.map((desafio) => (
            <DesafioCard key={desafio.id} desafio={desafio} />
          ))}

        <PrimaryButton
          label="Desafiar Amigo"
          icon={UsersIcon}
          onPress={desafiarAmigo}
        />
      </ScrollView>
    </View>
  );
}

export default DesafiosScreen;
