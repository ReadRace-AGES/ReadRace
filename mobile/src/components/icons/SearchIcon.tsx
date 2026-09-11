import Svg, { Circle, Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type SearchIconProps = {
  size?: number;
  color?: string;
  strokeWidth?: number;
};

/**
 * Lupa de traço fino usada no campo de busca e no EmptyState da Busca.
 * A lupa da barra de navegação é outro glifo e vive em TabIcons.
 */
export function SearchIcon({
  size = sizes.icon,
  color = colors.primary,
  strokeWidth = 2,
}: SearchIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx="11" cy="11" r="8" stroke={color} strokeWidth={strokeWidth} />
      <Path
        d="m21 21-4.3-4.3"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
      />
    </Svg>
  );
}
