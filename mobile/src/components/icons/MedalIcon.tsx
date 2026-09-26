import Svg, { Circle, Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type MedalIconProps = {
  size?: number;
  color?: string;
  strokeWidth?: number;
};

/** Medalha de traço fino, ao lado do título "Conquistas Desbloqueadas". */
export function MedalIcon({
  size = sizes.icon,
  color = colors.primary,
  strokeWidth = 2,
}: MedalIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M7.5 2.5h3l2 5M16.5 2.5h-3l-1 2.5"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <Circle cx={12} cy={14.5} r={6} stroke={color} strokeWidth={strokeWidth} />
      <Path
        d="M12 12v5"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
      />
    </Svg>
  );
}
