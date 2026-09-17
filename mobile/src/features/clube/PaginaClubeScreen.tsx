import { useCallback, useRef, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useRouter, type Href } from 'expo-router';
import Svg, { Path, Rect } from 'react-native-svg';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import {
  PrimaryButton,
  type PrimaryButtonIconProps,
} from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { buscarDetalhe, type LivroDetalhe } from '@/features/livro/api';
import { RegistrarProgressoSheet } from '@/features/progresso/RegistrarProgressoSheet';
import { colors, spacing, textStyles } from '@/theme';

import { RankingCard } from './RankingCard';
import { useClube } from './useClube';

const COPY = {
  registrarLeitura: 'Registrar leitura',
  acessarQuiz: 'Acessar Quiz',
  forum: 'Fórum de leitura',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar',
  erroAoAbrirRegistro:
    'Não foi possível abrir o registro de leitura. Tente novamente.',
} as const;

// O Fórum é a #36 e ainda não tem rota; o `as Href` é o mesmo recurso que a #34 usou para
// apontar para esta tela antes de ela existir.
function rotaDoForum(clubeId: string): Href {
  return `/clube/${clubeId}/forum` as Href;
}

function IconePlus({ size, color }: PrimaryButtonIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M12 5v14M5 12h14"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />
    </Svg>
  );
}

function IconeCheck({ size, color }: PrimaryButtonIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M20 6 9 17l-5-5"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

function IconeLista({ size, color }: PrimaryButtonIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Rect
        x={4}
        y={3}
        width={16}
        height={18}
        rx={2}
        stroke={color}
        strokeWidth={2}
      />
      <Path
        d="M8 8h8M8 12h8M8 16h5"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />
    </Svg>
  );
}

export function PaginaClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const { showToast } = useToastContext();
  const { clube, recarregar } = useClube(clubeId);
  const [livroDoModal, setLivroDoModal] = useState<LivroDetalhe | null>(null);
  const [abrindoModal, setAbrindoModal] = useState(false);
  const [erroDoModal, setErroDoModal] = useState<string | null>(null);
  const buscaDoLivro = useRef<AbortController | null>(null);

  const fecharModal = useCallback(() => {
    buscaDoLivro.current?.abort();
    setLivroDoModal(null);
  }, []);
  // Sair da tela fecha o modal e cancela uma busca em voo, para ele não subir depois.
  useFocusEffect(useCallback(() => () => fecharModal(), [fecharModal]));

  const dados = clube.situacao === 'sucesso' ? clube.dados : null;

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace('/feed');
  }

  /**
   * O contrato da #35 devolve só o id do livro atual, e o modal da #33 precisa de total de
   * páginas e progresso: o detalhe do livro (#31) entra aqui, sob demanda.
   */
  async function abrirRegistroDeLeitura() {
    if (!dados || abrindoModal) return;
    setAbrindoModal(true);
    setErroDoModal(null);
    const controller = new AbortController();
    buscaDoLivro.current = controller;
    try {
      const detalhe = await buscarDetalhe(
        dados.livroAtual.id,
        controller.signal
      );
      if (!controller.signal.aborted) setLivroDoModal(detalhe);
    } catch (erro: unknown) {
      if (!controller.signal.aborted) {
        setErroDoModal(
          erro instanceof ApiError ? erro.message : COPY.erroAoAbrirRegistro
        );
      }
    } finally {
      setAbrindoModal(false);
    }
  }

  const subtitulo = dados
    ? [dados.livroAtual.titulo, dados.livroAtual.autor]
        .filter(Boolean)
        .join(' - ')
    : undefined;

  if (clube.situacao === 'erro') {
    return (
      <View style={styles.tela}>
        <EmptyState
          icon={BookIcon}
          message={clube.mensagem}
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

  return (
    <View style={styles.tela}>
      <ScrollView contentContainerStyle={styles.rolagem}>
        <AppHeader
          compact
          showBack
          onBackPress={voltar}
          title={dados?.nome ?? ''}
          subtitle={subtitulo}
          titleOverflow="wrap"
          titleNumberOfLines={2}
        />

        <View style={styles.conteudo}>
          <View style={styles.acoes}>
            <PrimaryButton
              label={COPY.registrarLeitura}
              icon={IconePlus}
              disabled={!dados}
              loading={abrindoModal}
              onPress={abrirRegistroDeLeitura}
            />
            <PrimaryButton
              label={COPY.acessarQuiz}
              icon={IconeCheck}
              disabled={!dados}
              onPress={showToast}
            />
            <PrimaryButton
              label={COPY.forum}
              icon={IconeLista}
              disabled={!dados}
              onPress={() => router.push(rotaDoForum(clubeId))}
            />
          </View>

          {erroDoModal && (
            <Text accessibilityRole="alert" style={styles.erroDoModal}>
              {erroDoModal}
            </Text>
          )}

          <RankingCard
            ranking={dados?.ranking ?? null}
            onLinhaPress={showToast}
            onVerTodosPress={showToast}
          />
        </View>
      </ScrollView>

      {livroDoModal && (
        <RegistrarProgressoSheet
          visible
          livroId={livroDoModal.livro.id}
          totalPaginas={livroDoModal.livro.totalPaginas}
          paginaAtual={livroDoModal.progresso?.paginaAtual ?? 0}
          paginaMaximaAlcancada={
            livroDoModal.progresso?.paginaMaximaAlcancada ?? 0
          }
          onClose={fecharModal}
          onSuccess={recarregar}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  rolagem: { flexGrow: 1, paddingBottom: spacing[10] },
  conteudo: { padding: spacing[6], gap: spacing[4] },
  acoes: { gap: spacing[4] },
  acoesDoErro: { alignSelf: 'stretch', gap: spacing[2] },
  erroDoModal: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: 'center',
  },
});

export default PaginaClubeScreen;
