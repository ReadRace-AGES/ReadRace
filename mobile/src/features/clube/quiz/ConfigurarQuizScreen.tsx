import { useState } from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';

import { AppHeader } from '@/components/AppHeader';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { Slider } from '@/components/Slider';
import { useToastContext } from '@/components/toast-provider';
import { useClube } from '@/features/clube/useClube';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

const TITULO_MAXIMO = 120;
const RECOMPENSA_MINIMA = 10;
const RECOMPENSA_MAXIMA = 500;
const RECOMPENSA_INICIAL = 100;

const COPY = {
  titulo: 'Configurar Quiz',
  apoio:
    'Defina as regras gerais do seu desafio literário antes de adicionar as perguntas.',
  secaoIdentificacao: 'IDENTIFICAÇÃO',
  campoTitulo: 'Título do Quiz',
  placeholderTitulo: 'Ex: Feitiços e Poções - Ano 1',
  livroBase: 'Livro Base',
  sagaSelecionada: 'Saga Selecionada',
  alterar: 'Alterar',
  secaoMecanicas: 'MECÂNICAS',
  recompensa: 'Recompensa de XP',
  recompensaApoio: 'Pontos ganhos por conclusão',
  proximo: 'Próximo: Adicionar Perguntas',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar',
} as const;

export type ConfigurarQuizScreenProps = {
  clubeId: string;
};

export function ConfigurarQuizScreen({ clubeId }: ConfigurarQuizScreenProps) {
  const router = useRouter();
  const { showToast } = useToastContext();
  const { clube, recarregar } = useClube(clubeId);
  const [titulo, setTitulo] = useState('');
  const [recompensaXp, setRecompensaXp] = useState(RECOMPENSA_INICIAL);

  const dados = clube.situacao === 'sucesso' ? clube.dados : null;
  const tituloValido = titulo.trim().length > 0;

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace(`/clube/${clubeId}`);
  }

  function avancar() {
    if (!tituloValido) return;
    router.push({
      pathname: '/clube/[clubeId]/quiz/adicionar-perguntas',
      params: {
        clubeId,
        titulo: titulo.trim(),
        recompensaXp: String(recompensaXp),
      },
    });
  }

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
      <AppHeader
        variant="surface"
        titleAlign="center"
        showBack
        onBackPress={voltar}
        title={COPY.titulo}
      />

      <View style={styles.conteudo}>
        <Text style={styles.apoio}>{COPY.apoio}</Text>

        <Text style={styles.secao}>{COPY.secaoIdentificacao}</Text>

        <View style={styles.campo}>
          <Text style={styles.rotulo}>{COPY.campoTitulo}</Text>
          <TextInput
            value={titulo}
            onChangeText={setTitulo}
            placeholder={COPY.placeholderTitulo}
            placeholderTextColor={colors.textSecondary}
            maxLength={TITULO_MAXIMO}
            style={styles.input}
            accessibilityLabel={COPY.campoTitulo}
          />
        </View>

        <View style={styles.campo}>
          <Text style={styles.rotulo}>{COPY.livroBase}</Text>
          <Card>
            <Text style={styles.sagaSelecionada}>{COPY.sagaSelecionada}</Text>
            {clube.situacao === 'carregando' ? (
              <ActivityIndicator
                color={colors.primary}
                style={styles.carregandoLivro}
              />
            ) : (
              <>
                <Text style={styles.tituloLivro}>
                  {dados?.livroAtual.titulo}
                </Text>
                {dados?.livroAtual.autor ? (
                  <Text style={styles.autorLivro}>
                    {dados.livroAtual.autor}
                  </Text>
                ) : null}
              </>
            )}
            <PrimaryButton
              label={COPY.alterar}
              variant="outline"
              onPress={showToast}
            />
          </Card>
        </View>

        <Text style={styles.secao}>{COPY.secaoMecanicas}</Text>

        <Card>
          <View style={styles.cabecalhoRecompensa}>
            <View style={styles.textosRecompensa}>
              <Text style={styles.tituloRecompensa}>{COPY.recompensa}</Text>
              <Text style={styles.apoioRecompensa}>{COPY.recompensaApoio}</Text>
            </View>
            <View
              style={styles.valorRecompensa}
              accessibilityLiveRegion="polite"
            >
              <Text style={styles.numeroRecompensa}>{recompensaXp}</Text>
              <Text style={styles.unidadeRecompensa}>XP</Text>
            </View>
          </View>

          <View style={styles.divisor} />

          <Slider
            minimumValue={RECOMPENSA_MINIMA}
            maximumValue={RECOMPENSA_MAXIMA}
            value={recompensaXp}
            minimumLabel="10 XP"
            middleLabel="250 XP"
            maximumLabel="500 XP"
            accessibilityLabel={COPY.recompensa}
            onValueChange={setRecompensaXp}
          />
        </Card>

        <PrimaryButton
          label={COPY.proximo}
          disabled={!tituloValido}
          onPress={avancar}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  conteudo: { padding: spacing[6], gap: spacing[4] },
  apoio: { ...textStyles.bodySmall, color: colors.textSecondary },
  secao: {
    ...textStyles.captionStrong,
    color: colors.textSecondary,
    letterSpacing: 1,
  },
  campo: { gap: spacing[2] },
  rotulo: { ...textStyles.bodySmallStrong, color: colors.text },
  input: {
    ...textStyles.body,
    color: colors.text,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
  },
  sagaSelecionada: {
    ...textStyles.captionStrong,
    color: colors.primarySoft,
    marginBottom: spacing[1],
  },
  tituloLivro: { ...textStyles.bodyStrong, color: colors.text },
  autorLivro: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
    marginBottom: spacing[3],
  },
  carregandoLivro: { alignSelf: 'flex-start', marginVertical: spacing[3] },
  cabecalhoRecompensa: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  textosRecompensa: { flex: 1, gap: spacing[1] },
  tituloRecompensa: { ...textStyles.bodyStrong, color: colors.text },
  apoioRecompensa: { ...textStyles.bodySmall, color: colors.textSecondary },
  valorRecompensa: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: spacing[1],
  },
  numeroRecompensa: { ...textStyles.display, color: colors.primary },
  unidadeRecompensa: { ...textStyles.bodySmall, color: colors.textSecondary },
  divisor: {
    height: sizes.borderWidth,
    backgroundColor: colors.borderStrong,
    marginVertical: spacing[4],
  },
  acoesDoErro: { alignSelf: 'stretch', gap: spacing[2] },
});

export default ConfigurarQuizScreen;
