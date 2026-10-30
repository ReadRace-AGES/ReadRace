import { Text, View } from 'react-native';

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
};

/**
 * Card da aba Social (#197): quanto alguém que eu sigo leu de um livro em um dia. Não é um post
 * gravado, então não tem curtir nem comentar. O cabeçalho é o mesmo do `PostCard`.
 */
export function AtividadeCard({ atividade }: AtividadeCardProps) {
  const { usuario } = atividade;

  return (
    <Card>
      <View className="flex-row items-center gap-3">
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
      </View>

      <View className="mt-3 gap-1 rounded-md bg-surface-pink p-3">
        <Text className="text-body font-inter-bold text-primary">
          {tituloDaAtividade(atividade)}
        </Text>
        <Text numberOfLines={2} className="text-bodySmall font-inter text-text">
          {linhaDoLivro(atividade)}
        </Text>
        <Text className="text-caption font-inter-semibold text-text-secondary">
          {linhaDeNumeros(atividade)}
        </Text>
      </View>
    </Card>
  );
}

export default AtividadeCard;
