/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cue: {
          primary:   '#1a365d',
          secondary: '#2b6cb0',
          accent:    '#3182ce',
          light:     '#ebf8ff',
          success:   '#276749',
          warning:   '#975a16',
          danger:    '#9b2c2c',
        },
      },
    },
  },
  plugins: [],
}
