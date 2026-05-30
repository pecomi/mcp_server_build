/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        r5: 'rgba(188, 15, 29, 0.05)',
        r10: 'rgba(188, 15, 29, 0.10)',
        r20: 'rgba(188, 15, 29, 0.20)',
        r40: 'rgba(188, 15, 29, 0.40)',
        r60: 'rgba(188, 15, 29, 0.60)',
        r80: 'rgba(188, 15, 29, 0.80)',
        r100: '#bc0f1d',
      },
      fontFamily: {
        sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'Apple SD Gothic Neo', 'Noto Sans CJK KR', 'sans-serif'],
        mono: ['Pretendard', 'ui-monospace', 'SFMono-Regular', 'Consolas', 'monospace'],
      },
    },
  },
  plugins: [],
};
