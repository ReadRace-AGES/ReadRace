import Svg, { Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type BookIconProps = {
  size?: number;
  color?: string;
  strokeWidth?: number;
};

/**
 * Livro aberto de traço fino para o EmptyState da biblioteca.
 * O livro da barra de navegação é outro glifo e vive em TabIcons.
 */
export function BookIcon({
  size = sizes.icon,
  color = colors.primary,
  strokeWidth = 2,
}: BookIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M12 6.5C10.5 5 8.5 4.5 4 4.5v13c4.5 0 6.5.5 8 2 1.5-1.5 3.5-2 8-2v-13c-4.5 0-6.5.5-8 2Z"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinejoin="round"
      />
      <Path
        d="M12 6.5v13"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
      />
    </Svg>
  );
}
