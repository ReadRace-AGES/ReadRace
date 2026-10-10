import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useRouter, type Href } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import {
  colors,
  radius,
  sizes,
  spacing,
  textStyles,
  typography,
} from '@/theme';

import {
  buscarQuiz,
  ERRO_QUIZ,
  responderPergunta,
  type AlternativaQuiz,
  type Quiz,
  type RespostaQuiz,
} from './api';
import { marcarQuizConcluido, marcarRespostaNoClube } from './retornoDoQuiz';

const COPY = {
  apoio:
    'Responda a pergunta corretamente para ganhar pontos extras dentro da corrida!',
  correta: 'RESPOSTA CORRETA!',
  incorreta: 'RESPOSTA INCORRETA!',
  naoDesista: 'Não desista!',
  continuar: 'CONTINUAR',
  jaRespondeu: 'Você já respondeu o quiz deste clube.',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar para o clube',
  erroAoCarregar: 'Não foi possível carregar o quiz.',
  erroAoResponder: 'Não foi possível enviar sua resposta. Tente novamente.',
} as const;

type Carga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; quiz: Quiz };

/** A alternativa escolhida e o que a API decidiu sobre ela. */
type Resultado = { alternativaId: string; resposta: RespostaQuiz };

function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

function mensagemDe(erro: unknown, padrao: string) {
  return erro instanceof ApiError ? erro.message : padrao;
}

function IconeCheck({ color }: { color: string }) {
  return (
    <Svg width={sizes.icon} height={sizes.icon} viewBox="0 0 24 24" fill="none">
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

function IconeX({ color }: { color: string }) {
  return (
    <Svg width={sizes.icon} height={sizes.icon} viewBox="0 0 24 24" fill="none">
      <Path
        d="M18 6 6 18M6 6l12 12"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />
    </Svg>
  );
}

/**
 * O membro responde o quiz do clube uma pergunta por vez (#162).
 *
 * A tela nunca sabe qual é a correta antes de responder: a API decide e devolve só se acertou.
 * No erro, a correta não é revelada, como no Figma. "CONTINUAR" busca de novo o quiz, que já
 * devolve a próxima pergunta não respondida; depois da última, volta à Página do clube.
 */
export function QuizScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { showErrorToast } = useToastContext();
  const [carga, setCarga] = useState<Carga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);
  const [enviando, setEnviando] = useState<string | null>(null);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setCarga({ situacao: 'carregando' });
    setResultado(null);
    buscarQuiz(clubeId, controller.signal)
      .then((quiz) => {
        if (!controller.signal.aborted) setCarga({ situacao: 'sucesso', quiz });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setCarga({
          situacao: 'erro',
          mensagem: mensagemDe(erro, COPY.erroAoCarregar),
        });
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace(rotaDoClube(clubeId));
  }

  const quiz = carga.situacao === 'sucesso' ? carga.quiz : null;
  const pergunta = quiz?.proximaPergunta ?? null;

  async function escolher(alternativa: AlternativaQuiz) {
    // Um toque por pergunta: nada aceita segundo toque até a resposta voltar.
    if (!pergunta || enviando || resultado) return;
    setEnviando(alternativa.id);
    try {
      const resposta = await responderPergunta(
        clubeId,
        pergunta.id,
        alternativa.id
      );
      marcarRespostaNoClube(clubeId);
      if (resposta.concluiu && resposta.resumo) {
        marcarQuizConcluido(clubeId, resposta.resumo);
      }
      setResultado({ alternativaId: alternativa.id, resposta });
    } catch (erro: unknown) {
      showErrorToast(mensagemDe(erro, COPY.erroAoResponder));
      // Respondida em outro lugar (outra aba, envio repetido): segue para a próxima.
      if (erro instanceof ApiError && erro.code === ERRO_QUIZ.jaRespondida) {
        marcarRespostaNoClube(clubeId);
        recarregar();
      }
    } finally {
      setEnviando(null);
    }
  }

  function continuar() {
    if (resultado?.resposta.concluiu) {
      voltar();
      return;
    }
    recarregar();
  }

  if (carga.situacao === 'erro') {
    return (
      <View style={styles.tela}>
        <EmptyState
          icon={BookIcon}
          message={carga.mensagem}
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
          variant="surface"
          showBack
          onBackPress={voltar}
          title={quiz ? `Quiz - ${quiz.livro.titulo}` : ''}
          titleOverflow="wrap"
          titleNumberOfLines={2}
        />

        <View style={styles.conteudo}>
          <Text style={styles.apoio}>{COPY.apoio}</Text>

          {quiz === null ? (
            <View
              style={styles.carregando}
              accessibilityLabel="Carregando pergunta"
            >
              <ActivityIndicator color={colors.primary} />
            </View>
          ) : pergunta === null ? (
            // O botão da Página do clube avisa antes de abrir; só chega aqui por link direto.
            <EmptyState icon={BookIcon} message={COPY.jaRespondeu} />
          ) : (
            <>
              <Text accessibilityRole="header" style={styles.enunciado}>
                {pergunta.enunciado}
              </Text>
              <View style={styles.alternativas}>
                {pergunta.alternativas.map((alternativa) => (
                  <BotaoAlternativa
                    key={alternativa.id}
                    alternativa={alternativa}
                    estado={estadoDa(alternativa.id, resultado)}
                    enviando={enviando === alternativa.id}
                    bloqueada={enviando !== null || resultado !== null}
                    onPress={() => escolher(alternativa)}
                  />
                ))}
              </View>
            </>
          )}
        </View>
      </ScrollView>

      {resultado && (
        <PainelResultado
          acertou={resultado.resposta.acertou}
          pontosGanhos={resultado.resposta.pontosGanhos}
          paddingInferior={insets.bottom}
          onContinuar={continuar}
        />
      )}
    </View>
  );
}

type EstadoAlternativa = 'neutra' | 'correta' | 'errada';

function estadoDa(
  alternativaId: string,
  resultado: Resultado | null
): EstadoAlternativa {
  if (!resultado || resultado.alternativaId !== alternativaId) return 'neutra';
  return resultado.resposta.acertou ? 'correta' : 'errada';
}

function BotaoAlternativa({
  alternativa,
  estado,
  enviando,
  bloqueada,
  onPress,
}: {
  alternativa: AlternativaQuiz;
  estado: EstadoAlternativa;
  enviando: boolean;
  bloqueada: boolean;
  onPress: () => void;
}) {
  const cor =
    estado === 'correta'
      ? colors.quizCorreto
      : estado === 'errada'
        ? colors.quizErrado
        : undefined;
  // `style` como função some sob o NativeWind; o pressionado vem de estado, como no PrimaryButton.
  const [pressionada, setPressionada] = useState(false);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${alternativa.letra}: ${alternativa.texto}`}
      accessibilityState={{
        disabled: bloqueada,
        busy: enviando,
        selected: estado !== 'neutra',
      }}
      disabled={bloqueada}
      onPress={onPress}
      onPressIn={() => setPressionada(true)}
      onPressOut={() => setPressionada(false)}
      style={[
        styles.alternativa,
        cor !== undefined && { borderColor: cor, borderWidth: 2 },
        pressionada && styles.pressionada,
      ]}
    >
      <Text
        style={[styles.textoAlternativa, cor !== undefined && { color: cor }]}
      >
        {alternativa.texto}
      </Text>
      {enviando && <ActivityIndicator color={colors.primary} />}
      {estado === 'correta' && <IconeCheck color={colors.quizCorreto} />}
      {estado === 'errada' && <IconeX color={colors.quizErrado} />}
    </Pressable>
  );
}

function PainelResultado({
  acertou,
  pontosGanhos,
  paddingInferior,
  onContinuar,
}: {
  acertou: boolean;
  pontosGanhos: number;
  paddingInferior: number;
  onContinuar: () => void;
}) {
  const cor = acertou ? colors.quizCorreto : colors.quizErradoTexto;

  return (
    <View
      accessibilityLiveRegion="polite"
      style={[
        styles.painel,
        {
          backgroundColor: acertou
            ? colors.quizCorretoFundo
            : colors.quizErradoFundo,
          paddingBottom: paddingInferior + spacing[8],
        },
      ]}
    >
      <View style={[styles.selo, { borderColor: cor }]}>
        <Text style={[styles.textoDoSelo, { color: cor }]}>
          {acertou ? COPY.correta : COPY.incorreta}
        </Text>
      </View>
      <Text style={[styles.detalhe, { color: cor }]}>
        {acertou ? `+ ${pontosGanhos} PONTOS` : COPY.naoDesista}
      </Text>
      <Pressable
        accessibilityRole="button"
        onPress={onContinuar}
        hitSlop={spacing[3]}
      >
        <Text style={[styles.continuar, { color: cor }]}>{COPY.continuar}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  rolagem: { flexGrow: 1, paddingBottom: spacing[10] },
  conteudo: {
    flexGrow: 1,
    paddingHorizontal: spacing[8],
    gap: spacing[6],
  },
  apoio: { ...textStyles.bodySmall, color: colors.text },
  enunciado: { ...textStyles.bodyStrong, color: colors.primary },
  carregando: { flexGrow: 1, justifyContent: 'center', alignItems: 'center' },
  alternativas: { gap: spacing[5] },
  alternativa: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    minHeight: sizes.inputHeight,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: radius.sm,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    backgroundColor: colors.surface,
  },
  textoAlternativa: {
    ...textStyles.bodySmall,
    flex: 1,
    color: colors.textSecondary,
  },
  pressionada: { opacity: 0.8 },
  acoesDoErro: { alignSelf: 'stretch', gap: spacing[2] },
  painel: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    gap: spacing[3],
    paddingTop: spacing[8],
    paddingHorizontal: spacing[10],
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
  },
  selo: {
    alignSelf: 'stretch',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: sizes.buttonHeight,
    borderWidth: 2,
    borderRadius: radius.pill,
  },
  textoDoSelo: {
    fontFamily: typography.fontFamily.extrabold,
    fontSize: typography.fontSize.bodySmall,
  },
  detalhe: { ...textStyles.caption },
  continuar: {
    ...textStyles.bodySmallStrong,
    marginTop: spacing[4],
  },
});

export default QuizScreen;
