import { Pressable, Text, View } from 'react-native';

import { Avatar, StreakBadge } from '@/components/avatar';
import { Card } from '@/components/Card';
import { tempoRelativo } from '@/features/livro/model';
import { sizes } from '@/theme';

import type { AtividadeSocial } from './api';
import {
  linhaDeNumeros,
  linhaDoLivro,
  tituloDaAtividade,
} from './atividadeSocial';

export type AtividadeCardProps = {
  atividade: AtividadeSocial;
  /** Toque no avatar ou no nome: abre o perfil de quem leu (#204). */
  onAutorPress: () => void;
  /** Toque no livro: abre o detalhe do livro (#204). */
  onLivroPress: () => void;
};

/**
 * Card da aba Social: quanto alguém que eu sigo leu de um livro em um dia (#197) ou o livro que
 * essa pessoa terminou (#204). Não é um post gravado, então não tem curtir nem comentar. O
 * cabeçalho é o mesmo do `PostCard`.
 */
export function AtividadeCard({
  atividade,
  onAutorPress,
  onLivroPress,
}: AtividadeCardProps) {
  const { usuario } = atividade;

  return (
    <Card>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Abrir o perfil de ${usuario.nome}`}
        onPress={onAutorPress}
        className="flex-row items-center gap-3"
      >
        <Avatar
          name={usuario.nome}
          photoUrl={usuario.avatarUrl}
          size={sizes.avatar}
        />
        <View className="flex-1">
          <View className="flex-row items-center gap-2">
            <Text
              numberOfLines={1}
              className="shrink text-body font-inter-bold text-text"
            >
              {usuario.nome}
            </Text>
            {usuario.sequenciaDias != null && (
              <StreakBadge value={usuario.sequenciaDias} />
            )}
          </View>
          <Text className="text-caption font-inter text-text-secondary">
            {tempoRelativo(atividade.em)}
          </Text>
        </View>
      </Pressable>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Abrir o livro ${atividade.livro.titulo}`}
        onPress={onLivroPress}
        className="mt-3 gap-1 rounded-md bg-surface-pink p-3 active:bg-surface-pink-strong"
      >
        <Text className="text-body font-inter-bold text-primary">
          {tituloDaAtividade(atividade)}
        </Text>
        <Text numberOfLines={2} className="text-bodySmall font-inter text-text">
          {linhaDoLivro(atividade)}
        </Text>
        <Text className="text-caption font-inter-semibold text-text-secondary">
          {linhaDeNumeros(atividade)}
        </Text>
      </Pressable>
    </Card>
  );
}

export default AtividadeCard;
