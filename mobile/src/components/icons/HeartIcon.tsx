import Svg, { Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type HeartIconProps = {
  filled: boolean;
  size?: number;
};

/** Coração da curtida: cheio quando curtido por mim (#101). Post e comentário (#150). */
export function HeartIcon({ filled, size = sizes.iconSmall }: HeartIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24">
      <Path
        d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        fill={filled ? colors.accent : 'none'}
        stroke={colors.accent}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

export default HeartIcon;
