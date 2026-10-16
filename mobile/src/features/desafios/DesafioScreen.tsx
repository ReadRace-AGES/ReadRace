import { useCallback, useRef, useState } from 'react';

import { useFocusEffect, useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { Avatar } from '@/components/avatar';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { RematchIcon } from '@/components/icons/RematchIcon';
import { SadFaceIcon } from '@/components/icons/SadFaceIcon';
import { TrophyIcon } from '@/components/icons/TrophyIcon';
import { UsersIcon } from '@/components/icons/UsersIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, sizes, spacing, textStyles } from '@/theme';

import { listarTodosOsDesafios, type Desafio } from './api';

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'sucesso'; desafios: Desafio[] }
  | { situacao: 'erro'; mensagem: string; desafios?: Desafio[] };

type DesafioCardProps = {
  desafio: Desafio;
  onRevanche: (desafio: Desafio) => void;
};

function desafioFinalizado(desafio: Desafio) {
  return (
    desafio.status === 'concluido_ganho' ||
    desafio.status === 'concluido_perdido' ||
    desafio.status === 'concluido_empate'
  );
}

function DesafioCard({ desafio, onRevanche }: DesafioCardProps) {
  const finalizado = desafioFinalizado(desafio);

  const voceEstaNaFrente = desafio.progresso.voce > desafio.progresso.oponente;

  const oponenteEstaNaFrente =
    desafio.progresso.oponente > desafio.progresso.voce;

  const destacarVoce = finalizado
    ? desafio.status === 'concluido_ganho'
    : voceEstaNaFrente;

  const destacarOponente = finalizado
    ? desafio.status === 'concluido_perdido'
    : oponenteEstaNaFrente;

  function renderizarPilha() {
    if (!finalizado) {
      return (
        <Text style={[textStyles.micro, { color: colors.text }]}>
          {desafio.diasRestantes} {desafio.diasRestantes === 1 ? 'dia' : 'dias'}
        </Text>
      );
    }

    if (desafio.status === 'concluido_ganho') {
      return (
        <View className="flex-row items-center" style={{ gap: spacing[1] }}>
          <TrophyIcon size={sizes.iconSmall} color={colors.success} />

          <Text style={[textStyles.micro, { color: colors.success }]}>
            Vitória
          </Text>
        </View>
      );
    }

    if (desafio.status === 'concluido_perdido') {
      return (
        <View className="flex-row items-center" style={{ gap: spacing[1] }}>
          <SadFaceIcon size={sizes.iconSmall} color={colors.danger} />

          <Text style={[textStyles.micro, { color: colors.danger }]}>
            Derrota
          </Text>
        </View>
      );
    }

    // A task determina que o Figma não possui copy para empate.
    return null;
  }

  function corBordaPilha() {
    if (desafio.status === 'concluido_ganho') {
      return colors.success;
    }

    if (desafio.status === 'concluido_perdido') {
      return colors.danger;
    }

    return colors.borderStrong;
  }

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

          {desafio.status !== 'concluido_empate' && (
            <View
              style={{
                borderWidth: 1,
                borderColor: corBordaPilha(),
                borderRadius: 9999,
                paddingHorizontal: spacing[2],
                paddingVertical: spacing[1],
              }}
            >
              {renderizarPilha()}
            </View>
          )}
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
                  color: destacarVoce ? colors.primary : colors.text,
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
                  color: destacarOponente ? colors.primary : colors.text,
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

        {finalizado && (
          <PrimaryButton
            label="Revanche"
            variant="outline"
            icon={RematchIcon}
            onPress={() => onRevanche(desafio)}
          />
        )}
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

    setEstado((atual) =>
      atual.situacao === 'sucesso' ||
      (atual.situacao === 'erro' && atual.desafios !== undefined)
        ? atual
        : { situacao: 'carregando' }
    );

    try {
      const desafios = await listarTodosOsDesafios(controller.signal);

      if (controller.signal.aborted) return;

      setEstado({
        situacao: 'sucesso',
        desafios,
      });
    } catch (erro: unknown) {
      if (controller.signal.aborted) return;

      setEstado((atual) => ({
        situacao: 'erro',
        desafios: atual.situacao === 'carregando' ? undefined : atual.desafios,
        mensagem:
          erro instanceof ApiError
            ? erro.message
            : 'Não foi possível carregar os desafios.',
      }));
    } finally {
      if (requisicao.current === controller) {
        requisicao.current = null;
      }
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

  function desafiarAmigo(desafio?: Desafio) {
    if (!desafio) {
      router.push('/desafiar-amigo');
      return;
    }

    const parametros = {
      oponenteId: desafio.oponente.id,
      tipoMeta: desafio.tipoMeta,
      ...(desafio.tipoMeta === 'paginas'
        ? { meta: String(desafio.meta) }
        : {
            livroSelecionado: JSON.stringify({
              livroId: desafio.livro.id,
              titulo: desafio.livro.titulo,
              autor: desafio.livro.autor,
              capaUrl: desafio.livro.capaUrl,
              genero: null,
            }),
          }),
    };

    router.push({ pathname: '/desafiar-amigo', params: parametros });
  }

  const desafios =
    estado.situacao === 'carregando' ? undefined : estado.desafios;

  const listaVazia = estado.situacao === 'sucesso' && desafios?.length === 0;

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

        {desafios?.map((desafio) => (
          <DesafioCard
            key={desafio.id}
            desafio={desafio}
            onRevanche={desafiarAmigo}
          />
        ))}

        <View style={{ paddingHorizontal: spacing[4] }}>
          <PrimaryButton
            label="Desafiar Amigo"
            icon={UsersIcon}
            onPress={desafiarAmigo}
          />
        </View>
      </ScrollView>
    </View>
  );
}

export default DesafiosScreen;
