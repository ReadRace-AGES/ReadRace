import { Activity, useCallback, useState, type ReactNode } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { mensagemErroCarregamento } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors } from '@/theme';

import { buscarClube } from './api';

/** Não monta as telas exclusivas (nem seus hooks) antes de consultar a associação atual. */
export function AcessoClube({
  clubeId,
  administrador = false,
  children,
}: {
  clubeId: string;
  administrador?: boolean;
  children: ReactNode;
}) {
  const router = useRouter();
  const [tentativa, setTentativa] = useState(0);
  const [estado, setEstado] = useState<{
    id: string;
    administrador: boolean;
    permitido: boolean;
    verificando: boolean;
    erro?: string;
  } | null>(null);
  useFocusEffect(
    useCallback(() => {
      const controller = new AbortController();
      setEstado((atual) =>
        atual?.id === clubeId && atual.administrador === administrador
          ? { ...atual, verificando: true, erro: undefined }
          : null
      );
      buscarClube(clubeId, controller.signal)
        .then((clube) => {
          if (controller.signal.aborted) return;
          setEstado({
            id: clubeId,
            administrador,
            verificando: false,
            permitido:
              clube.situacao === 'membro' &&
              (!administrador || clube.meuCargo === 'administrador'),
          });
        })
        .catch((erro: unknown) => {
          if (!controller.signal.aborted)
            setEstado((atual) => ({
              id: clubeId,
              administrador,
              verificando: false,
              permitido:
                atual?.id === clubeId &&
                atual.administrador === administrador &&
                atual.permitido,
              erro: mensagemErroCarregamento(erro),
            }));
        });
      return () => {
        controller.abort();
        setEstado((atual) => (atual ? { ...atual, verificando: true } : atual));
      };
    }, [clubeId, administrador, tentativa])
  );

  const destaRota =
    estado?.id === clubeId && estado.administrador === administrador;
  const permitido = destaRota && estado.permitido;
  const visivel = permitido && !estado.verificando && !estado.erro;
  return (
    <View className="flex-1 bg-surface">
      {/* Activity preserva rascunhos e suspende os efeitos enquanto o cargo é revalidado.
          Antes da primeira autorização, o conteúdo sequer é renderizado. */}
      <Activity
        key={`${clubeId}/${administrador}`}
        mode={visivel ? 'visible' : 'hidden'}
      >
        {permitido ? children : null}
      </Activity>
      {!visivel && (
        <>
          <AppHeader
            title="Clube"
            variant="surface"
            showBack
            onBackPress={() => router.replace(`/clube/${clubeId}`)}
          />
          {!destaRota || estado.verificando ? (
            <ActivityIndicator
              accessibilityLabel="Verificando associação"
              color={colors.primary}
            />
          ) : (
            <EmptyState
              icon={BookIcon}
              messageStyle={estado.erro ? { color: colors.danger } : undefined}
              message={
                estado.erro ??
                (administrador
                  ? 'Só administradores do clube podem acessar esta tela.'
                  : 'Só membros do clube podem acessar esta tela.')
              }
              action={
                <PrimaryButton
                  label={estado.erro ? 'Tentar de novo' : 'Página do clube'}
                  onPress={() =>
                    estado.erro
                      ? setTentativa((v) => v + 1)
                      : router.replace(`/clube/${clubeId}`)
                  }
                  variant="outline"
                />
              }
            />
          )}
        </>
      )}
    </View>
  );
}
