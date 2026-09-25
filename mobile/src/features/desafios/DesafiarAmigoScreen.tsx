import { useEffect, useState } from 'react';
import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppHeader } from '@/components/AppHeader';
import { Avatar } from '@/components/avatar';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { SearchInput } from '@/components/SearchInput';
import { Slider } from '@/components/Slider';
import {
  colors,
  radius,
  sizes,
  spacing,
  textStyles,
  typography,
} from '@/theme';

import type { Oponente } from './api';
import { CrossedSwordsIcon, PencilIcon } from './DesafiarAmigoIcons';
import { useCriarDesafio, useOponentes } from './useDesafiarAmigo';

const META_INICIAL = 150;
const META_MINIMA = 10;
const META_MAXIMA = 500;
const PRAZO_INICIAL = 7;
const PRAZO_MAXIMO = 2_147_483_647;

type TipoMeta = 'paginas' | 'livro';

function prazoValido(valor: string) {
  if (!/^\d+$/.test(valor)) return null;

  const numero = Number(valor);
  return Number.isSafeInteger(numero) && numero > 0 && numero <= PRAZO_MAXIMO
    ? numero
    : null;
}

function metaValida(valor: number) {
  return (
    Number.isSafeInteger(valor) && valor >= META_MINIMA && valor <= META_MAXIMA
  );
}

function BotaoTentarDeNovo({ onPress }: { onPress: () => void }) {
  return (
    <PrimaryButton label="Tentar de novo" variant="outline" onPress={onPress} />
  );
}

function OponenteItem({
  oponente,
  selecionado,
  onPress,
}: {
  oponente: Oponente;
  selecionado: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={oponente.username}
      accessibilityState={{ selected: selecionado }}
      onPress={onPress}
      style={styles.oponente}
    >
      <View
        style={[
          styles.anelAvatar,
          selecionado ? styles.anelSelecionado : styles.anelNaoSelecionado,
        ]}
      >
        <Avatar
          name={oponente.username}
          photoUrl={oponente.avatarUrl}
          size={sizes.avatarLarge}
        />
      </View>
      <Text
        numberOfLines={1}
        style={[
          styles.username,
          selecionado
            ? styles.usernameSelecionado
            : styles.usernameNaoSelecionado,
        ]}
      >
        {oponente.username}
      </Text>
    </Pressable>
  );
}

function CartaoTipoMeta({
  label,
  selecionado,
  onPress,
}: {
  label: string;
  selecionado: boolean;
  onPress: () => void;
}) {
  const cor = selecionado ? colors.primary : colors.textSecondary;

  return (
    <Card
      accessibilityLabel={label}
      accessibilityState={{ selected: selecionado }}
      onPress={onPress}
      style={styles.cartaoTipo}
      surfaceStyle={[
        styles.superficieTipo,
        selecionado
          ? styles.superficieTipoSelecionada
          : styles.superficieTipoNaoSelecionada,
      ]}
    >
      <BookIcon size={sizes.icon} color={cor} />
      <Text
        style={[
          selecionado ? textStyles.bodyStrong : textStyles.body,
          { color: cor },
        ]}
      >
        {label}
      </Text>
    </Card>
  );
}

export function DesafiarAmigoScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [termo, setTermo] = useState('');
  const [oponenteId, setOponenteId] = useState<string | null>(null);
  const [tipoMeta, setTipoMeta] = useState<TipoMeta>('paginas');
  const [meta, setMeta] = useState(META_INICIAL);
  const [prazoDias, setPrazoDias] = useState(PRAZO_INICIAL);
  const [prazoDigitado, setPrazoDigitado] = useState(String(PRAZO_INICIAL));
  const [editandoPrazo, setEditandoPrazo] = useState(false);
  const { estado: estadoOponentes, recarregar } = useOponentes(termo);
  const { estado: estadoEnvio, enviar, limparErro } = useCriarDesafio();

  const oponenteSelecionado =
    oponenteId !== null &&
    estadoOponentes.situacao === 'sucesso' &&
    estadoOponentes.oponentes.some((oponente) => oponente.id === oponenteId);

  useEffect(() => {
    if (
      oponenteId !== null &&
      estadoOponentes.situacao === 'sucesso' &&
      !oponenteSelecionado
    ) {
      setOponenteId(null);
    }
  }, [estadoOponentes, oponenteId, oponenteSelecionado]);

  const prazoAtual = prazoValido(prazoDigitado);
  const podeEnviar =
    oponenteSelecionado &&
    tipoMeta === 'paginas' &&
    metaValida(meta) &&
    prazoAtual !== null &&
    estadoEnvio.situacao !== 'enviando';

  function voltarParaDesafios() {
    router.replace('/desafios');
  }

  function alterarTermo(valor: string) {
    setTermo(valor);
    limparErro();
  }

  function selecionarOponente(id: string) {
    setOponenteId(id);
    limparErro();
  }

  function selecionarTipo(tipo: TipoMeta) {
    setTipoMeta(tipo);
    limparErro();
  }

  function alterarMeta(valor: number) {
    setMeta(valor);
    limparErro();
  }

  function alterarPrazo(valor: string) {
    setPrazoDigitado(valor);
    const valido = prazoValido(valor);
    if (valido !== null) setPrazoDias(valido);
    limparErro();
  }

  function terminarEdicaoDoPrazo() {
    const valido = prazoValido(prazoDigitado);
    if (valido === null) {
      setPrazoDigitado(String(prazoDias));
    } else {
      setPrazoDias(valido);
    }
    setEditandoPrazo(false);
  }

  async function enviarDesafio() {
    if (!podeEnviar || !oponenteId || prazoAtual === null) return;

    const sucesso = await enviar({
      oponenteId,
      tipoMeta: 'paginas',
      meta,
      prazoDias: prazoAtual,
    });

    if (sucesso) router.replace('/desafios');
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        keyboardDismissMode="on-drag"
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={[
          styles.rolagem,
          { paddingBottom: insets.bottom + spacing[8] },
        ]}
      >
        <View style={styles.cabecalho}>
          <AppHeader
            compact
            title="Desafiar"
            subtitle="Escolha um oponente e defina a meta"
            variant="primary"
            titleAlign="center"
            showBack
            onBackPress={voltarParaDesafios}
          />
        </View>

        <View style={styles.conteudo}>
          <View style={styles.secao}>
            <Text style={styles.tituloSecao}>Oponente</Text>
            <SearchInput
              accessibilityLabel="Buscar oponente pelo nome de usuário"
              placeholder="Buscar pelo @nome..."
              value={termo}
              onChangeText={alterarTermo}
              variant="outlined"
              returnKeyType="search"
            />

            {estadoOponentes.situacao === 'carregando' && (
              <View style={styles.estadoOponentes}>
                <ActivityIndicator
                  color={colors.primary}
                  accessibilityLabel="Carregando oponentes"
                />
              </View>
            )}

            {estadoOponentes.situacao === 'erro' && (
              <View style={styles.estadoOponentes}>
                <Text accessibilityRole="alert" style={styles.erroBusca}>
                  {estadoOponentes.mensagem}
                </Text>
                <BotaoTentarDeNovo onPress={recarregar} />
              </View>
            )}

            {estadoOponentes.situacao === 'sucesso' &&
              (estadoOponentes.oponentes.length === 0 ? (
                <View style={styles.estadoOponentes}>
                  <EmptyState
                    icon={BookIcon}
                    message="Nenhum amigo encontrado."
                  />
                </View>
              ) : (
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={styles.listaOponentes}
                  accessibilityLabel="Oponentes"
                >
                  {estadoOponentes.oponentes.map((oponente) => (
                    <OponenteItem
                      key={oponente.id}
                      oponente={oponente}
                      selecionado={oponente.id === oponenteId}
                      onPress={() => selecionarOponente(oponente.id)}
                    />
                  ))}
                </ScrollView>
              ))}
            {estadoOponentes.situacao === 'sucesso' &&
              estadoOponentes.oponentes.length > 0 &&
              !oponenteSelecionado && (
                <Text
                  accessibilityLiveRegion="polite"
                  style={styles.indicacaoOponente}
                >
                  Escolha um oponente
                </Text>
              )}
          </View>

          <View style={styles.secao}>
            <Text style={styles.tituloSecao}>Tipo de Meta</Text>
            <View style={styles.tiposMeta}>
              <CartaoTipoMeta
                label="Páginas"
                selecionado={tipoMeta === 'paginas'}
                onPress={() => selecionarTipo('paginas')}
              />
              <CartaoTipoMeta
                label="Livro Específico"
                selecionado={tipoMeta === 'livro'}
                onPress={() => selecionarTipo('livro')}
              />
            </View>
          </View>

          {tipoMeta === 'paginas' && (
            <Card
              style={styles.cartaoVolume}
              surfaceStyle={styles.superficieVolume}
            >
              <View style={styles.cabecalhoVolume}>
                <View style={styles.textosVolume}>
                  <Text style={styles.tituloVolume}>Volume de Leitura</Text>
                  {editandoPrazo ? (
                    <View style={styles.prazoEditavel}>
                      <Text style={styles.textoPrazo}>Em</Text>
                      <TextInput
                        autoFocus
                        accessibilityLabel="Prazo em dias corridos"
                        keyboardType="number-pad"
                        inputMode="numeric"
                        maxLength={String(PRAZO_MAXIMO).length}
                        value={prazoDigitado}
                        onChangeText={alterarPrazo}
                        onBlur={terminarEdicaoDoPrazo}
                        onSubmitEditing={terminarEdicaoDoPrazo}
                        selectTextOnFocus
                        style={styles.campoPrazo}
                      />
                      <Text style={styles.textoPrazo}>dias corridos</Text>
                    </View>
                  ) : (
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel={`Editar prazo. Em ${prazoDias} dias corridos`}
                      onPress={() => setEditandoPrazo(true)}
                      hitSlop={spacing[2]}
                      style={styles.prazo}
                    >
                      <Text style={styles.textoPrazo}>
                        Em {prazoDias} dias corridos
                      </Text>
                      <PencilIcon
                        size={sizes.iconSmall}
                        color={colors.textSecondary}
                      />
                    </Pressable>
                  )}
                </View>

                <View style={styles.valorMeta} accessibilityLiveRegion="polite">
                  <Text style={styles.numeroMeta}>{meta}</Text>
                  <Text style={styles.unidadeMeta}>pág</Text>
                </View>
              </View>

              <View style={styles.divisor} />

              <Slider
                minimumValue={META_MINIMA}
                maximumValue={META_MAXIMA}
                value={meta}
                minimumLabel="10"
                maximumLabel="500+"
                accessibilityLabel="Meta de páginas"
                onValueChange={alterarMeta}
              />
            </Card>
          )}

          {estadoEnvio.situacao === 'erro' && (
            <Text accessibilityRole="alert" style={styles.erroEnvio}>
              {estadoEnvio.mensagem}
            </Text>
          )}

          <PrimaryButton
            label="Enviar Desafio"
            icon={CrossedSwordsIcon}
            disabled={!podeEnviar}
            loading={estadoEnvio.situacao === 'enviando'}
            onPress={enviarDesafio}
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surfaceMuted },
  rolagem: { flexGrow: 1 },
  cabecalho: {
    overflow: 'hidden',
    backgroundColor: colors.primary,
    borderBottomLeftRadius: radius.xl,
    borderBottomRightRadius: radius.xl,
  },
  conteudo: {
    paddingHorizontal: spacing[6],
    paddingTop: spacing[6],
    gap: spacing[8],
  },
  secao: { gap: spacing[4] },
  tituloSecao: { ...textStyles.h1, color: colors.text },
  estadoOponentes: {
    minHeight: sizes.avatarLarge * 2,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[4],
  },
  listaOponentes: { gap: spacing[6], paddingHorizontal: spacing[1] },
  oponente: {
    width: spacing[10] * 2,
    alignItems: 'center',
    gap: spacing[2],
  },
  anelAvatar: {
    padding: spacing[1],
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
  },
  anelSelecionado: {
    borderWidth: sizes.borderWidth * 2,
    borderColor: colors.primary,
    borderStyle: 'solid',
  },
  anelNaoSelecionado: {
    borderWidth: sizes.borderWidth * 2,
    borderColor: colors.textMuted,
    borderStyle: 'dashed',
  },
  username: {
    ...textStyles.bodySmall,
    alignSelf: 'stretch',
    textAlign: 'center',
  },
  usernameSelecionado: {
    fontFamily: typography.fontFamily.bold,
    color: colors.text,
  },
  usernameNaoSelecionado: { color: colors.textSecondary },
  tiposMeta: { flexDirection: 'row', gap: spacing[4] },
  cartaoTipo: {
    flex: 1,
    shadowOpacity: spacing[0],
    elevation: spacing[0],
  },
  superficieTipo: {
    minHeight: sizes.buttonHeight * 2,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    borderWidth: sizes.borderWidth,
  },
  superficieTipoSelecionada: {
    backgroundColor: colors.surfacePink,
    borderColor: colors.primary,
    borderWidth: sizes.borderWidth * 2,
  },
  superficieTipoNaoSelecionada: {
    backgroundColor: colors.surface,
    borderColor: colors.borderStrong,
  },
  cartaoVolume: {
    shadowOpacity: spacing[0],
    elevation: spacing[0],
    backgroundColor: colors.surface,
  },
  superficieVolume: {
    padding: spacing[6],
    gap: spacing[4],
    backgroundColor: colors.surface,
    borderWidth: sizes.borderWidth,
    borderColor: colors.surfacePinkStrong,
  },
  cabecalhoVolume: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[4],
  },
  textosVolume: { flex: 1, minWidth: 0, gap: spacing[1] },
  tituloVolume: { ...textStyles.h1, color: colors.text },
  prazo: { flexDirection: 'row', alignItems: 'center', gap: spacing[1] },
  textoPrazo: { ...textStyles.bodySmall, color: colors.textSecondary },
  prazoEditavel: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
  },
  campoPrazo: {
    ...textStyles.bodySmallStrong,
    minWidth: spacing[8],
    paddingHorizontal: spacing[1],
    paddingVertical: spacing[0],
    color: colors.text,
    borderBottomWidth: sizes.borderWidth,
    borderBottomColor: colors.primary,
    textAlign: 'center',
  },
  valorMeta: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: spacing[1],
    flexShrink: 0,
  },
  numeroMeta: { ...textStyles.display, color: colors.primary },
  unidadeMeta: { ...textStyles.bodySmall, color: colors.textSecondary },
  divisor: { height: sizes.borderWidth, backgroundColor: colors.borderStrong },
  erroBusca: {
    ...textStyles.body,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  indicacaoOponente: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
  },
  erroEnvio: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: 'center',
  },
});

export default DesafiarAmigoScreen;
