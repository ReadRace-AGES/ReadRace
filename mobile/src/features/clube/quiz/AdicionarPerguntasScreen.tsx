import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';
import Svg, { Path } from 'react-native-svg';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { Card } from '@/components/Card';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import { publicarQuiz, type PerguntaNova } from './api';

const LETRAS = ['A', 'B', 'C', 'D'] as const;
const REDIRECIONAMENTO_MS = 1200;

const COPY = {
  titulo: 'Adicionar Perguntas',
  listaPerguntas: 'Perguntas',
  novaPergunta: 'Nova Pergunta',
  placeholderPergunta: 'Digite a pergunta aqui...',
  opcoes: 'Opções de Resposta (Selecione a correta)',
  adicionar: '+ Adicionar à Lista',
  avisoSubstituicao: 'Publicar este quiz substitui o quiz atual do clube.',
  finalizar: 'Finalizar Quiz',
  publicado: 'Quiz publicado!',
  resposta: 'Resposta: ',
  erroGenerico: 'Não foi possível publicar o quiz. Tente novamente.',
} as const;

function TrashIcon({ size, color }: { size: number; color: string }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M4 7h16M9 7V4h6v3m-9 0 1 13h10l1-13"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

type FormularioPergunta = {
  enunciado: string;
  alternativas: [string, string, string, string];
  correta: number | null;
};

const FORMULARIO_VAZIO: FormularioPergunta = {
  enunciado: '',
  alternativas: ['', '', '', ''],
  correta: null,
};

function formularioValido(formulario: FormularioPergunta): boolean {
  return (
    formulario.enunciado.trim().length > 0 &&
    formulario.alternativas.every(
      (alternativa) => alternativa.trim().length > 0
    ) &&
    formulario.correta !== null
  );
}

export type AdicionarPerguntasScreenProps = {
  clubeId: string;
  titulo: string;
  recompensaXp: number;
};

export function AdicionarPerguntasScreen({
  clubeId,
  titulo,
  recompensaXp,
}: AdicionarPerguntasScreenProps) {
  const router = useRouter();
  const [perguntas, setPerguntas] = useState<PerguntaNova[]>([]);
  const [formulario, setFormulario] =
    useState<FormularioPergunta>(FORMULARIO_VAZIO);
  const [publicando, setPublicando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [publicado, setPublicado] = useState(false);

  // Some sozinho depois de confirmar — sem exigir mais um toque do líder.
  useEffect(() => {
    if (!publicado) return;
    const temporizador = setTimeout(() => {
      router.dismissTo(`/clube/${clubeId}`);
    }, REDIRECIONAMENTO_MS);
    return () => clearTimeout(temporizador);
  }, [publicado, router, clubeId]);

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace(`/clube/${clubeId}`);
  }

  function alterarAlternativa(indice: number, texto: string) {
    setFormulario((atual) => {
      const alternativas = [
        ...atual.alternativas,
      ] as FormularioPergunta['alternativas'];
      alternativas[indice] = texto;
      return { ...atual, alternativas };
    });
  }

  function adicionarPergunta() {
    if (!formularioValido(formulario)) return;
    setPerguntas((atual) => [
      ...atual,
      {
        enunciado: formulario.enunciado.trim(),
        alternativas: formulario.alternativas.map((alternativa) =>
          alternativa.trim()
        ) as PerguntaNova['alternativas'],
        correta: formulario.correta as number,
      },
    ]);
    setFormulario(FORMULARIO_VAZIO);
  }

  function removerPergunta(indice: number) {
    setPerguntas((atual) => atual.filter((_, i) => i !== indice));
  }

  async function finalizarQuiz() {
    if (perguntas.length === 0 || publicando) return;
    setPublicando(true);
    setErro(null);
    try {
      await publicarQuiz(clubeId, { titulo, recompensaXp, perguntas });
      setPublicado(true);
    } catch (erroCapturado: unknown) {
      setErro(
        erroCapturado instanceof ApiError
          ? erroCapturado.message
          : COPY.erroGenerico
      );
    } finally {
      setPublicando(false);
    }
  }

  if (publicado) {
    return (
      <View style={styles.telaPublicado}>
        <Text style={styles.textoPublicado}>{COPY.publicado}</Text>
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
        {perguntas.length > 0 && (
          <View style={styles.listaPerguntas}>
            <Text style={styles.secao}>{COPY.listaPerguntas}</Text>
            {perguntas.map((pergunta, indice) => (
              <Card key={`${indice}-${pergunta.enunciado}`}>
                <View style={styles.linhaPergunta}>
                  <View style={styles.textoPergunta}>
                    <Text style={styles.numeroPergunta}>{indice + 1}.</Text>
                    <Text style={styles.enunciadoPergunta}>
                      {pergunta.enunciado}
                    </Text>
                    <Text style={styles.respostaPergunta}>
                      {COPY.resposta}
                      {pergunta.alternativas[pergunta.correta]}
                    </Text>
                  </View>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Remover pergunta ${indice + 1}`}
                    hitSlop={spacing[2]}
                    onPress={() => removerPergunta(indice)}
                  >
                    <TrashIcon
                      size={sizes.iconSmall}
                      color={colors.textSecondary}
                    />
                  </Pressable>
                </View>
              </Card>
            ))}
          </View>
        )}

        <Card>
          <Text style={styles.secao}>{COPY.novaPergunta}</Text>

          <View style={styles.campo}>
            <TextInput
              value={formulario.enunciado}
              onChangeText={(texto) =>
                setFormulario((atual) => ({ ...atual, enunciado: texto }))
              }
              placeholder={COPY.placeholderPergunta}
              placeholderTextColor={colors.textSecondary}
              multiline
              style={styles.inputPergunta}
              accessibilityLabel="Pergunta"
            />
          </View>

          <Text style={styles.rotuloOpcoes}>{COPY.opcoes}</Text>

          {LETRAS.map((letra, indice) => (
            <View key={letra} style={styles.linhaAlternativa}>
              <TextInput
                value={formulario.alternativas[indice]}
                onChangeText={(texto) => alterarAlternativa(indice, texto)}
                placeholder={`Opção ${letra}`}
                placeholderTextColor={colors.textSecondary}
                style={styles.inputAlternativa}
                accessibilityLabel={`Opção ${letra}`}
              />
              <Pressable
                accessibilityRole="radio"
                accessibilityState={{ checked: formulario.correta === indice }}
                accessibilityLabel={`Marcar opção ${letra} como correta`}
                hitSlop={spacing[2]}
                onPress={() =>
                  setFormulario((atual) => ({ ...atual, correta: indice }))
                }
                style={[
                  styles.radio,
                  formulario.correta === indice && styles.radioSelecionado,
                ]}
              />
            </View>
          ))}

          <PrimaryButton
            label={COPY.adicionar}
            variant="outline"
            disabled={!formularioValido(formulario)}
            onPress={adicionarPergunta}
          />
        </Card>

        {erro && (
          <Text accessibilityRole="alert" style={styles.erro}>
            {erro}
          </Text>
        )}

        <Text style={styles.avisoSubstituicao}>{COPY.avisoSubstituicao}</Text>

        <PrimaryButton
          label={COPY.finalizar}
          disabled={perguntas.length === 0}
          loading={publicando}
          onPress={finalizarQuiz}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  conteudo: { padding: spacing[6], gap: spacing[4] },
  secao: { ...textStyles.bodyStrong, color: colors.text },
  listaPerguntas: { gap: spacing[3] },
  linhaPergunta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: spacing[3],
  },
  textoPergunta: { flex: 1, gap: spacing[1] },
  numeroPergunta: { ...textStyles.captionStrong, color: colors.textSecondary },
  enunciadoPergunta: { ...textStyles.body, color: colors.text },
  respostaPergunta: { ...textStyles.bodySmall, color: colors.primarySoft },
  campo: { marginTop: spacing[3] },
  inputPergunta: {
    ...textStyles.body,
    color: colors.text,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    minHeight: sizes.buttonHeight * 2,
    textAlignVertical: 'top',
  },
  rotuloOpcoes: {
    ...textStyles.bodySmallStrong,
    color: colors.text,
    marginTop: spacing[4],
    marginBottom: spacing[2],
  },
  linhaAlternativa: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    marginBottom: spacing[2],
  },
  inputAlternativa: {
    ...textStyles.body,
    flex: 1,
    color: colors.text,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
  },
  radio: {
    width: sizes.iconSmall,
    height: sizes.iconSmall,
    borderRadius: radius.pill,
    borderWidth: sizes.borderWidth * 2,
    borderColor: colors.inputBorder,
  },
  radioSelecionado: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
  },
  erro: { ...textStyles.bodySmall, color: colors.accent, textAlign: 'center' },
  avisoSubstituicao: {
    ...textStyles.caption,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  telaPublicado: {
    flex: 1,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing[6],
  },
  textoPublicado: { ...textStyles.h2, color: colors.text },
});

export default AdicionarPerguntasScreen;