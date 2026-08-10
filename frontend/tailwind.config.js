/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        base: {
          950: '#0B0F19',
          900: '#0F1420',
          800: '#151B2B',
          700: '#1C2436',
          600: '#2A3348',
        },
        accent: {
          blue: '#3B82F6',
          blueDark: '#2563EB',
          blueLight: '#93C5FD',
        },
        neon: {
          green: '#22FFB0',
        },
        anomaly: {
          red: '#EF3B4E',
        },
      },
      fontFamily: {
        heading: ['"Plus Jakarta Sans"', 'sans-serif'],
        sans: ['Inter', 'sans-serif'],
      },
      boxShadow: {
        glow: '0 0 24px rgba(59,130,246,0.35)',
        'glow-green': '0 0 18px rgba(34,255,176,0.45)',
        'glow-red': '0 0 18px rgba(239,59,78,0.4)',
      },
      backgroundImage: {
        'grid-pattern':
          'linear-gradient(rgba(59,130,246,0.12) 1px, transparent 1px), linear-gradient(90deg, rgba(59,130,246,0.12) 1px, transparent 1px)',
      },
      backgroundSize: {
        grid: '48px 48px',
      },
      keyframes: {
        gridMove: {
          '0%': { backgroundPosition: '0 0' },
          '100%': { backgroundPosition: '48px 48px' },
        },
        pulseDot: {
          '0%, 100%': { opacity: 1, transform: 'scale(1)' },
          '50%': { opacity: 0.55, transform: 'scale(1.5)' },
        },
        floatSlow: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-14px)' },
        },
        magneticGlow: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(59,130,246,0.0)' },
          '50%': { boxShadow: '0 0 32px 4px rgba(59,130,246,0.25)' },
        },
      },
      animation: {
        'grid-move': 'gridMove 6s linear infinite',
        'pulse-dot': 'pulseDot 1.6s ease-in-out infinite',
        'float-slow': 'floatSlow 4s ease-in-out infinite',
        'magnetic-glow': 'magneticGlow 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
};
