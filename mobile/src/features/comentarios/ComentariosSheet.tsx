import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  useWindowDimensions,
} from 'react-native';

import { ApiError } from '@/api/client';
import { Avatar } from '@/components/avatar';
import { BottomSheet } from '@/components/BottomSheet';
import { HeartIcon } from '@/components/icons/HeartIcon';
import { SendIcon } from '@/components/icons/SendIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { textoPostValido } from '@/features/forum/model';
import { tempoRelativo } from '@/features/livro/model';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import type { Comentario } from './api';
import { idsComRespostas, totalDeRespostas } from './model';
import { useComentarios } from './useComentarios';

const LIMITE_COMENTARIO = 1000;
// A lista ocupa pelo menos esta fração da tela, mesmo com poucos comentários. Com o
// rodapé fixo do campo, o painel fica com uns 72% da tela.
const ALTURA_MINIMA = 0.62;
// A partir daqui as respostas não recuam mais, para o texto não ficar espremido.
const NIVEL_MAXIMO_DE_RECUO = 4;

const COPY = {
  titulo: 'Comentários',
  vazio: 'Nenhum comentário ainda.',
  naoSegue: 'Siga a comunidade para comentar.',
  naoMembro: 'Participe do clube para comentar.',
  naoPodeComentarLivro: 'Você não tem permissão para comentar neste post.',
  placeholder: 'Escreva um comentário...',
  placeholderResposta: 'Escreva uma resposta...',
  comentar: 'Comentar',
  responder: 'Responder',
  cancelar: 'Cancelar',
  recolherTudo: 'Recolher respostas',
  expandirTudo: 'Expandir respostas',
  tentarDeNovo: 'Tentar de novo',
  erroEnvio: 'Não foi possível comentar. Tente novamente.',
  erroCurtida: 'Não foi possível registrar sua curtida. Tente novamente.',
} as const;

type ComentariosSheetProps = {
  visible: boolean;
  postId: string | null;
  contexto?: 'comunidade' | 'clube' | 'livro';
  onClose: () => void;
  /** Depois de comentar ou responder, para a tela somar 1 no balão do post. */
  onComentado: (postId: string) => void;
};

type Respondendo = { id: string; nome: string };

/** Painel "Comentários" de um post de comunidade, em thread (#150). */
export function ComentariosSheet({
  visible,
  postId,
  onClose,
  onComentado,
  contexto = 'comunidade',
}: ComentariosSheetProps) {
  const { height } = useWindowDimensions();
  const { showErrorToast } = useToastContext();
  const { carga, recarregar, comentar, alternarCurtida, curtidasPendentes } =
    useComentarios(visible ? postId : null);
  const [texto, setTexto] = useState('');
  const [respondendo, setRespondendo] = useState<Respondendo | null>(null);
  const [recolhidos, setRecolhidos] = useState<Set<string>>(() => new Set());
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const envio = useRef<AbortController | null>(null);
  const campo = useRef<TextInput>(null);
  const podeEnviar = textoPostValido(texto, LIMITE_COMENTARIO) && !enviando;

  useEffect(
    () => () => {
      envio.current?.abort();
      envio.current = null;
    },
    []
  );

  /** Fechar descarta o texto e a resposta, como o Novo post. Não fecha no meio de um envio. */
  function fechar() {
    if (envio.current) return;
    setTexto('');
    setErro(null);
    setRespondendo(null);
    setRecolhidos(new Set());
    onClose();
  }

  function responder(comentario: Comentario) {
    if (envio.current) return;
    setRespondendo({ id: comentario.id, nome: comentario.autor.nome ?? '' });
    setErro(null);
    campo.current?.focus();
  }

  /** Recolhe todas as threads; se já estão todas recolhidas, expande todas. */
  function alternarTodas(ids: string[]) {
    setRecolhidos(
      ids.every((id) => recolhidos.has(id)) ? new Set() : new Set(ids)
    );
  }

  function alternarRecolhido(id: string) {
    setRecolhidos((atual) => {
      const proximo = new Set(atual);
      if (proximo.has(id)) proximo.delete(id);
      else proximo.add(id);
      return proximo;
    });
  }

  async function curtirComentario(id: string) {
    if (curtidasPendentes.has(id)) return;
    try {
      await alternarCurtida(id);
    } catch (falha) {
      showErrorToast(
        falha instanceof ApiError ? falha.message : COPY.erroCurtida
      );
    }
  }

  async function enviar() {
    if (envio.current || !podeEnviar || !postId) return;
    const controller = new AbortController();
    envio.current = controller;
    setEnviando(true);
    setErro(null);
    try {
      const paiId = respondendo?.id ?? null;
      await comentar(texto, paiId, controller.signal);
      if (controller.signal.aborted) return;
      setTexto('');
      setRespondendo(null);
      if (paiId) {
        setRecolhidos((atual) => {
          const proximo = new Set(atual);
          proximo.delete(paiId);
          return proximo;
        });
      }
      onComentado(postId);
    } catch (falha) {
      if (controller.signal.aborted) return;
      setErro(falha instanceof ApiError ? falha.message : COPY.erroEnvio);
    } finally {
      if (envio.current === controller) {
        envio.current = null;
        setEnviando(false);
      }
    }
  }

  function renderComentario(comentario: Comentario, nivel: number) {
    const respostas = totalDeRespostas(comentario);
    const recolhido = recolhidos.has(comentario.id);
    const pendente = curtidasPendentes.has(comentario.id);
    const curtidas = `${comentario.totalCurtidas} curtidas${
      comentario.curtidoPorMim ? ', curtido por você' : ''
    }`;

    return (
      <View
        key={comentario.id}
        style={
          nivel > 0 && nivel <= NIVEL_MAXIMO_DE_RECUO
            ? styles.resposta
            : undefined
        }
      >
        <View style={styles.comentario}>
          <Avatar
            name={comentario.autor.nome ?? ''}
            photoUrl={comentario.autor.avatarUrl}
            size={nivel === 0 ? sizes.avatar : sizes.icon}
          />
          <View style={styles.corpo}>
            <View style={styles.linhaAutor}>
              <Text numberOfLines={1} style={styles.autor}>
                {comentario.autor.nome ?? ''}
              </Text>
              <Text style={styles.tempo}>
                {tempoRelativo(comentario.publicadoEm)}
              </Text>
            </View>
            <Text style={styles.texto}>{comentario.texto}</Text>
            <View style={styles.acoes}>
              <Pressable
                accessibilityRole="button"
                accessibilityState={{
                  disabled: pendente,
                  selected: comentario.curtidoPorMim,
                }}
                accessibilityLabel={curtidas}
                disabled={pendente}
                onPress={() => curtirComentario(comentario.id)}
                hitSlop={8}
                style={styles.acao}
              >
                <HeartIcon filled={comentario.curtidoPorMim} />
                <Text style={styles.acaoTexto}>{comentario.totalCurtidas}</Text>
              </Pressable>
              {carga.situacao === 'sucesso' && carga.dados.podeComentar && (
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Responder a ${comentario.autor.nome ?? ''}`}
                  onPress={() => responder(comentario)}
                  hitSlop={8}
                  style={styles.acao}
                >
                  <Text style={styles.acaoTexto}>{COPY.responder}</Text>
                </Pressable>
              )}
              {respostas > 0 && (
                <Pressable
                  accessibilityRole="button"
                  accessibilityState={{ expanded: !recolhido }}
                  onPress={() => alternarRecolhido(comentario.id)}
                  hitSlop={8}
                  style={styles.acao}
                >
                  <Text style={styles.acaoTexto}>
                    {recolhido
                      ? `Mostrar ${respostas} ${respostas === 1 ? 'resposta' : 'respostas'}`
                      : 'Ocultar respostas'}
                  </Text>
                </Pressable>
              )}
            </View>
          </View>
        </View>
        {!recolhido &&
          comentario.respostas.map((resposta) =>
            renderComentario(resposta, nivel + 1)
          )}
      </View>
    );
  }

  // Fixo embaixo do painel, fora da rolagem: o campo não some quando a thread cresce.
  const rodape =
    carga.situacao !== 'sucesso' ? undefined : carga.dados.podeComentar ? (
      <View style={styles.campo}>
        {respondendo && (
          <View style={styles.respondendo}>
            <Text numberOfLines={1} style={styles.respondendoTexto}>
              Respondendo a {respondendo.nome}
            </Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Cancelar resposta"
              disabled={enviando}
              onPress={() => setRespondendo(null)}
              hitSlop={8}
            >
              <Text style={styles.acaoTexto}>{COPY.cancelar}</Text>
            </Pressable>
          </View>
        )}
        <View style={styles.linhaCampo}>
          <TextInput
            ref={campo}
            accessibilityLabel={
              respondendo ? 'Texto da resposta' : 'Texto do comentário'
            }
            placeholder={
              respondendo ? COPY.placeholderResposta : COPY.placeholder
            }
            placeholderTextColor={colors.textMuted}
            multiline
            editable={!enviando}
            value={texto}
            onChangeText={(valor) => {
              if (envio.current) return;
              setTexto(valor);
              setErro(null);
            }}
            style={styles.input}
          />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={respondendo ? COPY.responder : COPY.comentar}
            accessibilityState={{
              disabled: !podeEnviar,
              busy: enviando,
            }}
            disabled={!podeEnviar}
            onPress={enviar}
            hitSlop={4}
            style={[
              styles.enviar,
              podeEnviar ? styles.enviarAtivo : styles.enviarInativo,
            ]}
          >
            {enviando ? (
              <ActivityIndicator color={colors.textInverse} />
            ) : (
              <SendIcon
                size={sizes.iconSmall + spacing[1]}
                color={podeEnviar ? colors.textInverse : colors.textMuted}
              />
            )}
          </Pressable>
        </View>
        <Text style={styles.counter}>{texto.length}/1.000</Text>
        {erro && (
          <Text
            accessibilityRole="alert"
            accessibilityLiveRegion="polite"
            style={styles.error}
          >
            {erro}
          </Text>
        )}
      </View>
    ) : (
      <Text style={[styles.message, styles.status]}>
        {contexto === 'clube'
          ? COPY.naoMembro
          : contexto === 'livro'
            ? COPY.naoPodeComentarLivro
            : COPY.naoSegue}
      </Text>
    );

  return (
    <BottomSheet
      visible={visible}
      dismissible={!enviando}
      onClose={fechar}
      accessibilityLabel={COPY.titulo}
      footer={rodape}
    >
      <View style={[styles.container, { minHeight: height * ALTURA_MINIMA }]}>
        <View style={styles.header}>
          <View style={styles.headerLado} />
          <Text accessibilityRole="header" style={styles.title}>
            {COPY.titulo}
          </Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={enviando}
            onPress={fechar}
            hitSlop={spacing[3]}
            style={styles.headerLado}
          >
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
        </View>

        {carga.situacao === 'carregando' && (
          <View style={styles.status} accessibilityLabel="Carregando">
            <ActivityIndicator color={colors.primary} />
          </View>
        )}

        {carga.situacao === 'erro' && (
          <View style={styles.status}>
            <Text accessibilityRole="alert" style={styles.message}>
              {carga.mensagem}
            </Text>
            <PrimaryButton
              label={COPY.tentarDeNovo}
              variant="outline"
              onPress={recarregar}
            />
          </View>
        )}

        {carga.situacao === 'sucesso' && (
          <View style={styles.lista}>
            {(() => {
              const ids = idsComRespostas(carga.dados.comentarios);
              if (ids.length === 0) return null;
              const todasRecolhidas = ids.every((id) => recolhidos.has(id));
              return (
                <Pressable
                  accessibilityRole="button"
                  accessibilityState={{ expanded: !todasRecolhidas }}
                  onPress={() => alternarTodas(ids)}
                  hitSlop={8}
                  style={styles.alternarTodas}
                >
                  <Text style={styles.acaoTexto}>
                    {todasRecolhidas ? COPY.expandirTudo : COPY.recolherTudo}
                  </Text>
                </Pressable>
              );
            })()}
            {carga.dados.comentarios.length === 0 ? (
              <Text style={[styles.message, styles.status]}>{COPY.vazio}</Text>
            ) : (
              carga.dados.comentarios.map((comentario) =>
                renderComentario(comentario, 0)
              )
            )}
          </View>
        )}
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing[4] },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  // Mesma largura dos dois lados, para o título ficar no centro do painel.
  headerLado: {
    width: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    ...textStyles.h1,
    color: colors.primary,
    flex: 1,
    textAlign: 'center',
  },
  closeText: { ...textStyles.h1, color: colors.text },
  status: {
    alignItems: 'center',
    gap: spacing[3],
    paddingVertical: spacing[4],
  },
  message: {
    ...textStyles.body,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  lista: { gap: spacing[4] },
  comentario: { flexDirection: 'row', gap: spacing[3] },
  resposta: {
    marginTop: spacing[3],
    marginLeft: spacing[4],
    paddingLeft: spacing[3],
    borderLeftWidth: sizes.borderWidth * 2,
    borderLeftColor: colors.surfacePinkStrong,
  },
  corpo: { flex: 1, gap: spacing[1] },
  linhaAutor: { flexDirection: 'row', alignItems: 'center', gap: spacing[2] },
  autor: {
    ...textStyles.bodySmallStrong,
    color: colors.text,
    flexShrink: 1,
  },
  tempo: { ...textStyles.caption, color: colors.textSecondary },
  texto: { ...textStyles.bodySmall, color: colors.text },
  acoes: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: spacing[4],
    marginTop: spacing[1],
  },
  acao: { flexDirection: 'row', alignItems: 'center', gap: spacing[1] },
  acaoTexto: { ...textStyles.captionStrong, color: colors.textSecondary },
  campo: { gap: spacing[2] },
  alternarTodas: { alignSelf: 'flex-end' },
  // O botão fica embaixo, alinhado à última linha, quando o campo cresce.
  linhaCampo: { flexDirection: 'row', alignItems: 'flex-end', gap: spacing[2] },
  enviar: {
    width: sizes.inputHeight,
    height: sizes.inputHeight,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  enviarAtivo: { backgroundColor: colors.primary },
  enviarInativo: { backgroundColor: colors.surfaceDisabled },
  respondendo: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[2],
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: radius.sm,
    backgroundColor: colors.surfacePink,
  },
  respondendoTexto: {
    ...textStyles.caption,
    color: colors.text,
    flexShrink: 1,
  },
  input: {
    ...textStyles.body,
    flex: 1,
    minHeight: sizes.inputHeight,
    maxHeight: sizes.inputHeight * 3,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.sm,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    color: colors.text,
    textAlignVertical: 'top',
  },
  counter: {
    ...textStyles.caption,
    color: colors.textSecondary,
    textAlign: 'right',
  },
  error: { ...textStyles.bodySmall, color: colors.danger },
});
