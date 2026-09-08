import React from 'react';
import { ScrollView, Text, View } from 'react-native';

import { ReadRaceLogo } from '../components/readracelogo';
import { colors, radius, shadows, spacing, textStyles, typography } from '@/theme';

type ColorName = keyof typeof colors;

const COLOR_GROUPS: { title: string; names: ColorName[] }[] = [
  { title: 'Marca', names: ['primary', 'primarySoft', 'accent'] },
  {
    title: 'Superfícies',
    names: [
      'surface',
      'surfaceMuted',
      'surfaceAlt',
      'surfacePink',
      'surfacePinkStrong',
      'surfaceDisabled',
      'overlay',
      'chipOnPrimary',
    ],
  },
  { title: 'Bordas', names: ['border', 'borderStrong', 'inputBorder'] },
  { title: 'Texto', names: ['text', 'textSecondary', 'textMuted', 'textInverse'] },
  { title: 'Navegação', names: ['navInactiveStart', 'navInactive', 'navInactiveEnd'] },
  {
    title: 'Progresso e ranking',
    names: [
      'progressTrack',
      'rankGoldStart',
      'rankGoldEnd',
      'rankSilverStart',
      'rankSilverEnd',
      'rankBronzeStart',
      'rankBronzeEnd',
    ],
  },
];

const TEXT_SAMPLES: { name: keyof typeof textStyles; sample: string }[] = [
  { name: 'display', sample: '145' },
  { name: 'headerTitle', sample: 'Olá, Sate!' },
  { name: 'h1', sample: 'Olá, Sate!' },
  { name: 'h2', sample: 'Clube do Livro Quarta-Feira' },
  { name: 'h3', sample: 'Meus Grupos' },
  { name: 'bodyStrong', sample: 'Paulin KreyKrey' },
  { name: 'body', sample: 'Continue sua jornada literária' },
  { name: 'button', sample: 'Criar Grupo' },
  { name: 'bodySmallStrong', sample: 'Harry Potter e a Pedra Filosofal' },
  { name: 'bodySmall', sample: 'Um livro incrível, sinceramente; muito leve e descontraído.' },
  { name: 'captionStrong', sample: 'Ler mais' },
  { name: 'caption', sample: '3h atrás' },
  { name: 'microStrong', sample: '43 membros' },
  { name: 'micro', sample: 'R.J. Palacio' },
];

function SectionTitle({ children }: { children: string }) {
  return (
    <View className="mt-8 mb-3">
      <Text className="font-inter-bold text-micro uppercase tracking-wider text-primary">
        {children}
      </Text>
      <View className="mt-2 h-1 w-10 rounded-pill bg-accent" />
    </View>
  );
}

function Swatch({ name }: { name: ColorName }) {
  const value = colors[name];
  return (
    <View className="w-[30%]">
      <View className="h-10 rounded-md border border-border" style={{ backgroundColor: value }} />
      <Text className="mt-1 font-inter-semibold text-micro text-text">{name}</Text>
      <Text className="font-inter text-micro text-text-secondary">{value}</Text>
    </View>
  );
}

export default function IdentidadeVisualScreen() {
  return (
    <View className="flex-1 bg-surface">
      <View className="rounded-b-xl bg-primary px-6 pt-10 pb-6">
        <Text className="text-h1 font-inter-bold text-text-inverse">Identidade Visual</Text>
        <Text className="mt-1 text-body font-inter text-text-inverse">
          Tokens do tema extraídos do Figma
        </Text>
      </View>

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ paddingHorizontal: spacing[6], paddingBottom: spacing[10] }}
        showsVerticalScrollIndicator={false}
      >
        <SectionTitle>Logo</SectionTitle>
        <View className="items-center rounded-lg bg-primary px-6 pt-6 pb-5">
          <ReadRaceLogo size={104} />
          <Text className="mt-2 text-h2 font-inter-bold italic tracking-wide text-text-inverse">
            Read Race
          </Text>
          <Text className="mt-1 text-micro font-inter text-text-inverse">Sua leitura imersiva</Text>
        </View>

        {COLOR_GROUPS.map((group) => (
          <View key={group.title}>
            <SectionTitle>{`Cores · ${group.title}`}</SectionTitle>
            <View className="flex-row flex-wrap gap-3">
              {group.names.map((name) => (
                <Swatch key={name} name={name} />
              ))}
            </View>
          </View>
        ))}

        <SectionTitle>Tipografia</SectionTitle>
        <View className="rounded-lg bg-surface-muted px-4 pt-4 pb-5">
          <Text className="mb-3 text-caption font-inter text-text-secondary">
            {`${typography.fontFamily.regular} · ${typography.fontFamily.semibold} · ${typography.fontFamily.bold}`}
          </Text>
          {TEXT_SAMPLES.map(({ name, sample }) => (
            <View key={name} className="mb-3">
              <Text style={[textStyles[name], { color: colors.text }]} numberOfLines={2}>
                {sample}
              </Text>
              <Text className="text-micro font-inter text-text-secondary">
                {`${name} · ${textStyles[name].fontSize}/${textStyles[name].lineHeight}`}
              </Text>
            </View>
          ))}
        </View>

        <SectionTitle>Espaçamento</SectionTitle>
        <View className="gap-2">
          {Object.entries(spacing)
            .filter(([, value]) => value > 0)
            .map(([name, value]) => (
              <View key={name} className="flex-row items-center gap-3">
                <Text className="w-10 text-caption font-inter-semibold text-text">{name}</Text>
                <View className="h-4 rounded-xs bg-accent" style={{ width: value * 4 }} />
                <Text className="text-caption font-inter text-text-secondary">{`${value}px`}</Text>
              </View>
            ))}
        </View>

        <SectionTitle>Raios</SectionTitle>
        <View className="flex-row flex-wrap gap-3">
          {Object.entries(radius).map(([name, value]) => (
            <View key={name} className="items-center">
              <View className="h-10 w-10 bg-surface-pink-strong" style={{ borderRadius: value }} />
              <Text className="mt-1 text-micro font-inter-semibold text-text">{name}</Text>
              <Text className="text-micro font-inter text-text-secondary">{`${value}px`}</Text>
            </View>
          ))}
        </View>

        <SectionTitle>Sombras</SectionTitle>
        <View className="flex-row gap-6 px-6">
          {Object.entries(shadows).map(([name, value]) => (
            <View key={name} className="items-center">
              <View className="h-10 flex-1 rounded-pill bg-surface" style={value} />
              <Text className="mt-2 text-micro font-inter-semibold text-text">{name}</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}
