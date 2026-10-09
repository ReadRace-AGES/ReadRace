import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

import { atualizarPerfil, type AtualizacaoPerfil, type Perfil } from './api';
import { ConfiguracoesGeraisSheet } from './ConfiguracoesGeraisSheet';
import { usePerfil } from './usePerfil';

const TAMANHO_MAX_NOME = 120;
const FORMATO_USERNAME = /^[a-z0-9_]{3,50}$/;
const MSG_NOME = 'O nome é obrigatório e deve ter até 120 caracteres.';
const MSG_USERNAME =
  'Use de 3 a 50 caracteres: letras minúsculas sem acento, números ou sublinhado.';
const MSG_USERNAME_EM_USO = 'Esse nome de usuário já está em uso.';

export function ConfiguracoesPerfilScreen() {
  const router = useRouter();
  const { estado, recarregar } = usePerfil();
  const voltar = () =>
    router.canGoBack() ? router.back() : router.replace('/perfil');

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.scroll}
      keyboardShouldPersistTaps="handled"
    >
      <AppHeader
        compact
        titleAlign="center"
        title="Configurações de perfil"
        showBack
        onBackPress={voltar}
      />
      {estado.situacao === 'carregando' && (
        <ActivityIndicator
          style={styles.content}
          color={colors.primary}
          accessibilityLabel="Carregando perfil"
        />
      )}
      {estado.situacao === 'erro' && (
        <View style={styles.content}>
          <Text style={styles.erro}>{estado.mensagem}</Text>
          <PrimaryButton label="Tentar novamente" onPress={recarregar} />
        </View>
      )}
      {estado.situacao === 'sucesso' && (
        <Formulario perfil={estado.dados} aoConcluir={voltar} />
      )}
    </ScrollView>
  );
}

function Formulario({
  perfil,
  aoConcluir,
}: {
  perfil: Perfil;
  aoConcluir: () => void;
}) {
  const { showToast, showErrorToast } = useToastContext();
  const [nome, setNome] = useState(perfil.nome);
  const [username, setUsername] = useState(perfil.username);
  const [erroNome, setErroNome] = useState<string | null>(null);
  const [erroUsername, setErroUsername] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [geraisAberto, setGeraisAberto] = useState(false);

  async function concluir() {
    const nomeLimpo = nome.trim();
    const usernameLimpo = username.trim();
    const nomeInvalido =
      nomeLimpo === '' || nomeLimpo.length > TAMANHO_MAX_NOME;
    const usernameInvalido = !FORMATO_USERNAME.test(usernameLimpo);
    setErroNome(nomeInvalido ? MSG_NOME : null);
    setErroUsername(usernameInvalido ? MSG_USERNAME : null);
    if (nomeInvalido || usernameInvalido) return;

    const mudancas: AtualizacaoPerfil = {};
    if (nomeLimpo !== perfil.nome) mudancas.nome = nomeLimpo;
    if (usernameLimpo !== perfil.username) mudancas.username = usernameLimpo;
    if (Object.keys(mudancas).length === 0) {
      aoConcluir();
      return;
    }

    setSalvando(true);
    try {
      await atualizarPerfil(mudancas);
      aoConcluir();
      setTimeout(() => showErrorToast('Perfil atualizado.'), 300);
    } catch (erro) {
      if (erro instanceof ApiError && erro.code === 'USERNAME_EM_USO') {
        setErroUsername(MSG_USERNAME_EM_USO);
      } else if (erro instanceof ApiError && erro.code === 'PERFIL_INVALIDO') {
        showErrorToast(erro.message);
      } else {
        showErrorToast('Não foi possível salvar. Tente novamente.');
      }
    } finally {
      setSalvando(false);
    }
  }

  return (
    <View style={styles.content}>
      <Campo
        label="Nome"
        value={nome}
        onChangeText={(texto) => {
          setNome(texto);
          setErroNome(null);
        }}
        erro={erroNome}
      />
      <Campo
        label="Usuário"
        value={username}
        onChangeText={(texto) => {
          setUsername(texto);
          setErroUsername(null);
        }}
        erro={erroUsername}
        autoCapitalize="none"
      />
      {/* TODO: e-mail depende da decisão do time (a API ainda não devolve). */}
      <Campo label="E-mail" value="" editable={false} />
      <Opcao label="Avatar" onPress={showToast} />
      <Opcao label="Plano de fundo" onPress={showToast} />
      <Opcao
        label="Configurações gerais"
        onPress={() => setGeraisAberto(true)}
      />
      <PrimaryButton label="Concluir" onPress={concluir} loading={salvando} />
      <ConfiguracoesGeraisSheet
        visible={geraisAberto}
        onClose={() => setGeraisAberto(false)}
      />
    </View>
  );
}

type CampoProps = {
  label: string;
  value: string;
  onChangeText?: (texto: string) => void;
  erro?: string | null;
  editable?: boolean;
  autoCapitalize?: 'none' | 'sentences' | 'words';
};

function Campo({
  label,
  value,
  onChangeText,
  erro,
  editable = true,
  autoCapitalize,
}: CampoProps) {
  return (
    <View style={styles.campo}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        value={value}
        onChangeText={onChangeText}
        editable={editable}
        autoCapitalize={autoCapitalize}
        autoCorrect={false}
        style={[
          styles.input,
          !editable && styles.inputDesabilitado,
          erro ? styles.inputErro : null,
        ]}
      />
      {erro ? <Text style={styles.erro}>{erro}</Text> : null}
    </View>
  );
}

function Opcao({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.opcao}>
      <Text style={styles.opcaoTexto}>{label}</Text>
      <Text style={styles.opcaoSeta}>›</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  scroll: { flexGrow: 1, paddingBottom: sizes.navHeight + spacing[6] },
  content: {
    paddingTop: spacing[6],
    paddingHorizontal: spacing[5],
    gap: spacing[4],
  },
  campo: { gap: spacing[1] },
  label: { ...textStyles.bodySmallStrong, color: colors.text },
  input: {
    ...textStyles.body,
    color: colors.text,
    height: sizes.inputHeight,
    borderWidth: sizes.borderWidth,
    borderColor: colors.inputBorder,
    borderRadius: radius.md,
    paddingHorizontal: spacing[3],
  },
  inputDesabilitado: {
    backgroundColor: colors.surfaceDisabled,
    color: colors.textMuted,
  },
  inputErro: { borderColor: colors.danger },
  erro: { ...textStyles.caption, color: colors.danger },
  opcao: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing[3],
    borderBottomWidth: sizes.borderWidth,
    borderBottomColor: colors.border,
  },
  opcaoTexto: { ...textStyles.body, color: colors.text },
  opcaoSeta: { ...textStyles.h2, color: colors.textMuted },
});