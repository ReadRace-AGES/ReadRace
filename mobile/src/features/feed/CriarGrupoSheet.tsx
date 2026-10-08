import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { ApiError } from '@/api/client';
import { BottomSheet } from '@/components/BottomSheet';
import { BookCover } from '@/components/BookCover';
import { Avatar } from '@/components/avatar';
import { EmptyState } from '@/components/EmptyState';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { ListItem } from '@/components/ListItem';
import { PrimaryButton } from '@/components/PrimaryButton';
import type { LivroBusca, UsuarioBusca } from '@/features/busca/api';
import { useBusca } from '@/features/busca/useBusca';
import { usePerfil } from '@/features/perfil/usePerfil';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import { criarClube, type ClubeCriadoResponse } from './api';

export type CriarGrupoSheetProps = {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
};

export function CriarGrupoSheet({
  visible,
  onClose,
  onSuccess,
}: CriarGrupoSheetProps) {
  const [nome, setNome] = useState('');
  const [tituloLivro, setTituloLivro] = useState('');
  const [livroSelecionado, setLivroSelecionado] = useState<LivroBusca | null>(
    null
  );
  const [descricao, setDescricao] = useState('');
  const [membros, setMembros] = useState('');
  const [membrosSelecionados, setMembrosSelecionados] = useState<
    UsuarioBusca[]
  >([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [clubeCriado, setClubeCriado] = useState<ClubeCriadoResponse | null>(
    null
  );
  const submitting = useRef<AbortController | null>(null);
  const canSubmit =
    visible &&
    !clubeCriado &&
    !!nome.trim() &&
    nome.length <= 120 &&
    !!livroSelecionado &&
    !loading;
  const { estado: perfil, recarregar: recarregarPerfil } = usePerfil(
    undefined,
    visible && !clubeCriado
  );
  const { busca: buscaMembros, recarregar: recarregarMembros } = useBusca(
    visible && !clubeCriado ? membros : '',
    'usuarios'
  );
  const mostrarBuscaMembros = visible && !!membros.trim();
  const usuarios =
    perfil.situacao === 'sucesso' &&
    buscaMembros.situacao === 'sucesso' &&
    buscaMembros.dados.tipo === 'usuarios'
      ? buscaMembros.dados.itens.filter(
          (usuario) =>
            usuario.id !== perfil.dados.id &&
            !membrosSelecionados.some((membro) => membro.id === usuario.id)
        )
      : [];
  const { busca, recarregar } = useBusca(
    visible && !clubeCriado && !livroSelecionado ? tituloLivro : '',
    'livros'
  );
  const mostrarBusca = visible && !livroSelecionado && !!tituloLivro.trim();

  const limparFormulario = useCallback(() => {
    setNome('');
    setTituloLivro('');
    setLivroSelecionado(null);
    setDescricao('');
    setMembros('');
    setMembrosSelecionados([]);
    setLoading(false);
    setError(null);
    setClubeCriado(null);
  }, []);

  useEffect(() => {
    if (visible) limparFormulario();

    return () => {
      submitting.current?.abort();
      submitting.current = null;
    };
  }, [visible, limparFormulario]);

  async function submit() {
    if (submitting.current || !canSubmit || !livroSelecionado) return;

    const controller = new AbortController();
    submitting.current = controller;
    setLoading(true);
    setError(null);
    try {
      const response = await criarClube(
        {
          nome: nome.trim(),
          descricao: descricao.trim() || null,
          livroId: livroSelecionado.id,
          membros: membrosSelecionados
            .filter(
              (membro) =>
                perfil.situacao !== 'sucesso' || membro.id !== perfil.dados.id
            )
            .map((membro) => membro.id),
        },
        controller.signal
      );
      if (controller.signal.aborted) return;

      setClubeCriado(response);
      onSuccess?.();
    } catch (err) {
      if (controller.signal.aborted) return;

      setError(
        err instanceof ApiError
          ? err.message
          : 'Não foi possível criar o grupo. Tente novamente.'
      );
    } finally {
      if (submitting.current === controller) {
        submitting.current = null;
        setLoading(false);
      }
    }
  }

  function alterarTituloLivro(titulo: string) {
    setTituloLivro(titulo);
    setLivroSelecionado(null);
  }

  function selecionarLivro(livro: LivroBusca) {
    setLivroSelecionado(livro);
    setTituloLivro(livro.titulo);
  }

  function adicionarMembro(usuario: UsuarioBusca) {
    if (perfil.situacao !== 'sucesso' || usuario.id === perfil.dados.id) return;

    setMembrosSelecionados((atual) =>
      atual.some((membro) => membro.id === usuario.id)
        ? atual
        : [...atual, usuario]
    );
    setMembros('');
  }

  function removerMembro(id: string) {
    setMembrosSelecionados((atual) =>
      atual.filter((membro) => membro.id !== id)
    );
  }

  return (
    <BottomSheet
      visible={visible}
      dismissible={!loading}
      onClose={onClose}
      accessibilityLabel={
        clubeCriado
          ? 'Clube criado com sucesso!'
          : 'Criar novo Grupo de Leitura'
      }
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>
            {clubeCriado ? clubeCriado.nome : 'Criar novo Grupo de Leitura'}
          </Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={loading}
            onPress={onClose}
            hitSlop={spacing[3]}
            style={styles.closeButton}
          >
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
        </View>

        {clubeCriado ? (
          <View style={styles.confirmacao}>
            <BookCover
              size="featured"
              source={clubeCriado.livro.capaUrl}
              accessibilityLabel={`Capa de ${clubeCriado.livro.titulo}`}
            />
            <View
              style={styles.checkCircle}
              accessibilityElementsHidden
              importantForAccessibility="no-hide-descendants"
            >
              <Text style={styles.check}>✓</Text>
            </View>
            <Text
              accessibilityRole="alert"
              accessibilityLiveRegion="polite"
              style={styles.success}
            >
              Clube criado com sucesso!
            </Text>
            <PrimaryButton
              label="Criar novo clube"
              onPress={limparFormulario}
            />
          </View>
        ) : (
          <>
            <View style={styles.field}>
              <Text style={styles.label}>Nome do Grupo</Text>
              <TextInput
                accessibilityLabel="Nome do Grupo"
                editable={!loading}
                maxLength={120}
                value={nome}
                onChangeText={setNome}
                style={styles.input}
              />
            </View>

            <View style={styles.field}>
              <Text style={styles.label}>Título do Livro</Text>
              <TextInput
                accessibilityLabel="Título do Livro"
                editable={!loading}
                value={tituloLivro}
                onChangeText={alterarTituloLivro}
                style={styles.input}
              />
              {mostrarBusca && busca.situacao === 'carregando' && (
                <ActivityIndicator
                  color={colors.primary}
                  accessibilityLabel="Carregando"
                />
              )}
              {mostrarBusca && busca.situacao === 'erro' && (
                <EmptyState
                  icon={SearchIcon}
                  message={busca.mensagem}
                  action={
                    <PrimaryButton
                      label="Tentar de novo"
                      variant="outline"
                      onPress={recarregar}
                    />
                  }
                />
              )}
              {mostrarBusca &&
                busca.situacao === 'sucesso' &&
                busca.dados.tipo === 'livros' &&
                (busca.dados.itens.length === 0 ? (
                  <EmptyState
                    icon={SearchIcon}
                    message="Livro não encontrado. Adicione o livro em Meus Livros antes de criar o clube."
                  />
                ) : (
                  busca.dados.itens.map((livro) => (
                    <ListItem
                      key={livro.id}
                      variant="book"
                      title={livro.titulo}
                      subtitle={livro.autor ?? undefined}
                      imageUrl={livro.capa}
                      onPress={() => selecionarLivro(livro)}
                    />
                  ))
                ))}
            </View>

            <View style={styles.field}>
              <Text style={styles.label}>Autor</Text>
              <TextInput
                accessibilityLabel="Autor, somente leitura"
                accessibilityState={{ disabled: true }}
                editable={false}
                value={livroSelecionado?.autor ?? ''}
                style={[styles.input, styles.readOnlyInput]}
              />
            </View>

            <View style={styles.field}>
              <Text style={styles.label}>Descrição (opcional)</Text>
              <TextInput
                accessibilityLabel="Descrição, opcional"
                editable={!loading}
                multiline
                value={descricao}
                onChangeText={setDescricao}
                style={[styles.input, styles.descriptionInput]}
              />
            </View>

            <View style={styles.field}>
              <Text style={styles.label}>Adicionar membros</Text>
              <TextInput
                accessibilityLabel="Adicionar membros"
                editable={!loading}
                value={membros}
                onChangeText={setMembros}
                style={styles.input}
              />
              {mostrarBuscaMembros &&
                (buscaMembros.situacao === 'carregando' ||
                  perfil.situacao === 'carregando') && (
                  <ActivityIndicator
                    color={colors.primary}
                    accessibilityLabel="Carregando membros"
                  />
                )}
              {mostrarBuscaMembros && perfil.situacao === 'erro' && (
                <EmptyState
                  icon={SearchIcon}
                  message={perfil.mensagem}
                  action={
                    <PrimaryButton
                      label="Tentar de novo"
                      variant="outline"
                      onPress={recarregarPerfil}
                    />
                  }
                />
              )}
              {mostrarBuscaMembros &&
                perfil.situacao === 'sucesso' &&
                buscaMembros.situacao === 'erro' && (
                  <EmptyState
                    icon={SearchIcon}
                    message={buscaMembros.mensagem}
                    action={
                      <PrimaryButton
                        label="Tentar de novo"
                        variant="outline"
                        onPress={recarregarMembros}
                      />
                    }
                  />
                )}
              {mostrarBuscaMembros &&
                perfil.situacao === 'sucesso' &&
                buscaMembros.situacao === 'sucesso' &&
                (usuarios.length === 0 ? (
                  <EmptyState
                    icon={SearchIcon}
                    message="Nenhum usuário encontrado."
                  />
                ) : (
                  usuarios.map((usuario) => (
                    <Pressable
                      key={usuario.id}
                      accessibilityRole="button"
                      disabled={loading}
                      accessibilityLabel={`Adicionar ${usuario.nome}, @${usuario.username}`}
                      onPress={() => adicionarMembro(usuario)}
                      className="flex-row items-center gap-4 border-b border-border px-6 py-3 active:bg-surface-muted"
                    >
                      <Avatar
                        name={usuario.nome}
                        photoUrl={usuario.avatar}
                        size={sizes.avatarLarge}
                      />
                      <View className="min-w-0 flex-1 gap-1">
                        <Text numberOfLines={1} style={styles.label}>
                          {usuario.nome}
                        </Text>
                        <Text style={styles.username}>@{usuario.username}</Text>
                      </View>
                    </Pressable>
                  ))
                ))}
              {membrosSelecionados.map((membro) => (
                <View key={membro.id} style={styles.membro}>
                  <Avatar
                    name={membro.nome}
                    photoUrl={membro.avatar}
                    size={sizes.avatarLarge}
                  />
                  <View style={styles.membroTexto}>
                    <Text numberOfLines={1} style={styles.label}>
                      {membro.nome}
                    </Text>
                    <Text style={styles.username}>@{membro.username}</Text>
                  </View>
                  <Pressable
                    accessibilityRole="button"
                    disabled={loading}
                    accessibilityLabel={`Remover ${membro.nome}, @${membro.username}`}
                    onPress={() => removerMembro(membro.id)}
                    hitSlop={spacing[3]}
                    style={styles.closeButton}
                  >
                    <Text style={styles.closeText}>✕</Text>
                  </Pressable>
                </View>
              ))}
            </View>

            {error && (
              <Text
                accessibilityRole="alert"
                accessibilityLiveRegion="polite"
                style={styles.error}
              >
                {error}
              </Text>
            )}
            <PrimaryButton
              label="Criar Grupo"
              loading={loading}
              disabled={!canSubmit}
              onPress={submit}
            />
          </>
        )}
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing[5],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: spacing[4],
  },
  title: {
    ...textStyles.h1,
    color: colors.primary,
    flex: 1,
  },
  closeButton: {
    minWidth: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeText: {
    ...textStyles.h1,
    color: colors.text,
  },
  field: {
    gap: spacing[2],
  },
  confirmacao: {
    alignItems: 'center',
    gap: spacing[5],
  },
  checkCircle: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing[4],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  check: {
    ...textStyles.h1,
    color: colors.primary,
  },
  success: {
    ...textStyles.bodyStrong,
    color: colors.primary,
    textAlign: 'center',
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: 'center',
  },
  membro: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[4],
  },
  membroTexto: {
    flex: 1,
    gap: spacing[1],
  },
  username: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
  },
  label: {
    ...textStyles.bodySmallStrong,
    color: colors.text,
  },
  input: {
    ...textStyles.body,
    minHeight: sizes.inputHeight,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.sm,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    backgroundColor: colors.surface,
    color: colors.text,
  },
  readOnlyInput: {
    backgroundColor: colors.surfaceDisabled,
    color: colors.textMuted,
  },
  descriptionInput: {
    minHeight: sizes.inputHeight * 2,
    textAlignVertical: 'top',
  },
});
