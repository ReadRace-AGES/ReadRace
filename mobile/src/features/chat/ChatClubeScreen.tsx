import { useRef, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useRouter, type Href } from 'expo-router';
import Svg, { Path } from 'react-native-svg';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing, textStyles, typography } from '@/theme';

import type { MensagemClube } from './api';
import { useChatClube } from './useChatClube';

const COPY = {
  titulo: 'Chat do clube',
  placeholder: 'Escreva uma mensagem...',
  enviar: 'Enviar',
  tentandoEnviar: 'Enviando...',
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar para o clube',
  vazio: 'Nenhuma mensagem ainda. Comece a conversa!',
  soMembro: 'Só membros do clube participam do chat.',
} as const;

function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

function IconeChat() {
  return (
    <Svg width={48} height={48} viewBox="0 0 24 24" fill="none">
      <Path
        d="M21 12a8 8 0 0 1-8 8H6l-4 2 1.5-5A9 9 0 1 1 21 12Z"
        stroke={colors.primary}
        strokeWidth={1.8}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

function formatarHorario(data: string) {
  const horario = new Date(data);

  if (Number.isNaN(horario.getTime())) {
    return '';
  }

  return horario.toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

function AvatarMensagem({
  nome,
  avatarUrl,
}: {
  nome: string;
  avatarUrl: string | null;
}) {
  if (avatarUrl) {
    return (
      <Image
        source={{ uri: avatarUrl }}
        style={styles.avatar}
        accessibilityLabel={`Foto de ${nome}`}
      />
    );
  }

  const inicial = nome.trim().charAt(0).toUpperCase() || '?';

  return (
    <View style={styles.avatarFallback}>
      <Text style={styles.avatarInicial}>{inicial}</Text>
    </View>
  );
}

function BalaoMensagem({ mensagem }: { mensagem: MensagemClube }) {
  return (
    <View
      style={[
        styles.linhaMensagem,
        mensagem.minha
          ? styles.linhaMensagemMinha
          : styles.linhaMensagemOutro,
      ]}
    >
      {!mensagem.minha && (
        <AvatarMensagem
          nome={mensagem.autor.nome}
          avatarUrl={mensagem.autor.avatarUrl}
        />
      )}

      <View
        style={[
          styles.conteudoMensagem,
          mensagem.minha
            ? styles.conteudoMensagemMinha
            : styles.conteudoMensagemOutro,
        ]}
      >
        {!mensagem.minha && (
          <Text style={styles.nomeAutor}>{mensagem.autor.nome}</Text>
        )}

        <View
          style={[
            styles.balao,
            mensagem.minha ? styles.balaoMeu : styles.balaoOutro,
          ]}
        >
          <Text style={styles.textoMensagem}>{mensagem.texto}</Text>

          <Text style={styles.horario}>
            {formatarHorario(mensagem.enviadaEm)}
          </Text>
        </View>
      </View>
    </View>
  );
}

export function ChatClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const scrollRef = useRef<ScrollView>(null);

  const { chat, enviando, erroEnvio, enviar, recarregar } =
    useChatClube(clubeId);

  const [texto, setTexto] = useState('');

  const textoValido =
    texto.trim().length > 0 && texto.trim().length <= 1000;

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }

    router.replace(rotaDoClube(clubeId));
  }

  async function enviarMensagem() {
    if (!textoValido || enviando) {
      return;
    }

    const enviado = await enviar(texto);

    if (enviado) {
      setTexto('');

      requestAnimationFrame(() => {
        scrollRef.current?.scrollToEnd({ animated: true });
      });
    }
  }

  if (chat.situacao === 'erro') {
    return (
      <View style={styles.tela}>
        <EmptyState
          icon={IconeChat}
          message={chat.mensagem}
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

  if (chat.situacao === 'nao_membro') {
    return (
      <View style={styles.tela}>
        <AppHeader
          variant="surface"
          titleAlign="center"
          showBack
          onBackPress={voltar}
          title={COPY.titulo}
        />

        <View style={styles.estadoCentralizado}>
          <EmptyState
            icon={IconeChat}
            message={COPY.soMembro}
            action={
              <PrimaryButton
                label={COPY.voltar}
                onPress={voltar}
                variant="outline"
              />
            }
          />
        </View>
      </View>
    );
  }

  const mensagens = chat.mensagens;

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <AppHeader
        variant="surface"
        titleAlign="center"
        showBack
        onBackPress={voltar}
        title={COPY.titulo}
      />

      {chat.situacao === 'carregando' && mensagens.length === 0 ? (
        <View
          style={styles.estadoCentralizado}
          accessibilityLabel="Carregando mensagens"
        >
          <ActivityIndicator color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          ref={scrollRef}
          style={styles.lista}
          contentContainerStyle={[
            styles.listaConteudo,
            mensagens.length === 0 && styles.listaVazia,
          ]}
          keyboardShouldPersistTaps="handled"
          onContentSizeChange={() => {
            scrollRef.current?.scrollToEnd({ animated: false });
          }}
        >
          {mensagens.length === 0 ? (
            <EmptyState icon={IconeChat} message={COPY.vazio} />
          ) : (
            mensagens.map((mensagem) => (
              <BalaoMensagem key={mensagem.id} mensagem={mensagem} />
            ))
          )}
        </ScrollView>
      )}

      <View style={styles.areaEnvio}>
        {erroEnvio && (
          <Text accessibilityRole="alert" style={styles.erroEnvio}>
            {erroEnvio}
          </Text>
        )}

        <View style={styles.linhaEnvio}>
          <TextInput
            value={texto}
            onChangeText={setTexto}
            placeholder={COPY.placeholder}
            placeholderTextColor={colors.textSecondary}
            maxLength={1000}
            multiline
            editable={!enviando}
            style={styles.input}
            accessibilityLabel={COPY.placeholder}
          />

          <TouchableOpacity
            accessibilityRole="button"
            accessibilityLabel={COPY.enviar}
            disabled={!textoValido || enviando}
            onPress={() => void enviarMensagem()}
            style={[
              styles.botaoEnviar,
              (!textoValido || enviando) && styles.botaoEnviarDesabilitado,
            ]}
          >
            {enviando ? (
              <ActivityIndicator color={colors.surface} size="small" />
            ) : (
              <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
                <Path
                  d="M22 2 11 13"
                  stroke={colors.surface}
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <Path
                  d="m22 2-7 20-4-9-9-4 20-7Z"
                  stroke={colors.surface}
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
            )}
          </TouchableOpacity>
        </View>

        <Text style={styles.contador}>{texto.length}/1000</Text>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  tela: {
    flex: 1,
    backgroundColor: colors.surface,
  },

  lista: {
    flex: 1,
  },

  listaConteudo: {
    padding: spacing[4],
    gap: spacing[4],
  },

  listaVazia: {
    flexGrow: 1,
    justifyContent: 'center',
  },

  estadoCentralizado: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },

  acoesDoErro: {
    alignSelf: 'stretch',
    gap: spacing[2],
  },

  linhaMensagem: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing[2],
  },

  linhaMensagemMinha: {
    justifyContent: 'flex-end',
  },

  linhaMensagemOutro: {
    justifyContent: 'flex-start',
  },

  conteudoMensagem: {
    maxWidth: '78%',
  },

  conteudoMensagemMinha: {
    alignItems: 'flex-end',
  },

  conteudoMensagemOutro: {
    alignItems: 'flex-start',
  },

  nomeAutor: {
    ...textStyles.bodySmall,
    fontFamily: typography.fontFamily.bold,
    color: colors.text,
    marginBottom: spacing[1],
  },

  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },

  avatarFallback: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },

  avatarInicial: {
    color: colors.surface,
    fontFamily: typography.fontFamily.bold,
  },

  balao: {
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: 16,
    minWidth: 72,
  },

  balaoMeu: {
    backgroundColor: colors.primary,
  },

  balaoOutro: {
  backgroundColor: colors.surfaceMuted,
},

  textoMensagem: {
    ...textStyles.bodySmall,
    color: colors.text,
  },

  horario: {
    fontSize: 11,
    color: colors.textSecondary,
    marginTop: spacing[1],
    alignSelf: 'flex-end',
  },

  areaEnvio: {
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
    paddingHorizontal: spacing[4],
    paddingTop: spacing[3],
    paddingBottom: spacing[4],
    backgroundColor: colors.surface,
  },

  linhaEnvio: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing[2],
  },

  input: {
    flex: 1,
    minHeight: 44,
    maxHeight: 120,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    color: colors.text,
    textAlignVertical: 'top',
  },

  botaoEnviar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },

  botaoEnviarDesabilitado: {
    opacity: 0.4,
  },

  erroEnvio: {
    ...textStyles.bodySmall,
    color: colors.accent,
    marginBottom: spacing[2],
  },

  contador: {
    fontSize: 11,
    color: colors.textSecondary,
    textAlign: 'right',
    marginTop: spacing[1],
  },
});

export default ChatClubeScreen;