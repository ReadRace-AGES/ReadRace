import Svg, { Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type SendIconProps = {
  size?: number;
  color?: string;
  strokeWidth?: number;
};

/** Avião de papel: enviar comentário (#150). */
export function SendIcon({
  size = sizes.icon,
  color = colors.textInverse,
  strokeWidth = 2,
}: SendIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7Z"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

export default SendIcon;
