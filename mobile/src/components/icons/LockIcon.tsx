import Svg, { Path, Rect } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type LockIconProps = {
  size?: number;
  color?: string;
  strokeWidth?: number;
};

/** Cadeado de traço fino, ao lado do título "Próximas Conquistas". */
export function LockIcon({
  size = sizes.icon,
  color = colors.primary,
  strokeWidth = 2,
}: LockIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Rect
        x={4.5}
        y={10.5}
        width={15}
        height={10}
        rx={2}
        stroke={color}
        strokeWidth={strokeWidth}
      />
      <Path
        d="M8 10.5V7a4 4 0 0 1 8 0v3.5"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
      />
    </Svg>
  );
}
