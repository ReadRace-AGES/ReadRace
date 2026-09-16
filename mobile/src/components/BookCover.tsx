import { Image, type ImageSource } from 'expo-image';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { bookCover } from '@/theme';

export type BookCoverSize = 'grid' | 'featured' | 'thumbnail';
type CoverSource = ImageSource | string | number;

export type BookCoverProps =
  | {
      variant?: 'cover';
      size?: BookCoverSize;
      source?: CoverSource | null;
      accessibilityLabel: string;
      onPress?: () => void;
    }
  | {
      variant: 'add-favorite';
      onPress: () => void;
      size?: never;
      source?: never;
      accessibilityLabel?: string;
    };

// Uma nova fonte remonta esta instancia e permite recuperar uma capa que falhou.
function CoverImage({ source }: { source: CoverSource }) {
  const [failed, setFailed] = useState(false);
  if (failed) return null;

  return (
    <Image
      source={source}
      style={{ width: '100%', height: '100%' }}
      contentFit="cover"
      accessible={false}
      onError={() => setFailed(true)}
    />
  );
}

/** Apresenta a capa e comunica toques. Navegacao e Toast sao da tela. */
export function BookCover(props: BookCoverProps) {
  const adding = props.variant === 'add-favorite';
  const size = adding ? 'grid' : (props.size ?? 'grid');
  const frameStyle = {
    ...bookCover[size],
    borderRadius: bookCover.borderRadius,
    flexShrink: 0,
  };
  const label = props.accessibilityLabel ?? 'adicionar favorito';
  const source = props.source;
  const hasSource =
    source != null &&
    (typeof source === 'number' ||
      (typeof source === 'string'
        ? source.trim().length > 0
        : Boolean(source.uri?.trim())));
  const contents = adding ? (
    <View
      pointerEvents="none"
      className="flex-1 items-center justify-center px-2"
    >
      <Text className="text-center font-inter-bold text-caption text-text-inverse">
        adicionar favorito
      </Text>
      <Text className="text-center font-inter-bold text-h2 text-text-inverse">
        +
      </Text>
    </View>
  ) : hasSource ? (
    <CoverImage key={JSON.stringify(source)} source={source!} />
  ) : null;
  const className = `overflow-hidden ${adding ? 'bg-primary' : 'bg-surface-alt'}`;

  if (props.onPress) {
    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={label}
        onPress={props.onPress}
        style={frameStyle}
        className={className}
      >
        {contents}
      </Pressable>
    );
  }
  return (
    <View
      accessible
      accessibilityRole="image"
      accessibilityLabel={label}
      style={frameStyle}
      className={className}
    >
      {contents}
    </View>
  );
}
