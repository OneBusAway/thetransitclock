import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';
import path from 'node:path';

export default defineConfig({
  plugins: [tailwindcss()],
  build: {
    outDir: path.resolve(import.meta.dirname, 'target/frontend-dist'),
    emptyOutDir: true,
    rollupOptions: {
      input: {
        tailwind: path.resolve(import.meta.dirname, 'frontend/tailwind.css'),
      },
      output: {
        // includes.jsp references /dist/tailwind.css by literal name.
        assetFileNames: '[name][extname]',
      },
    },
  },
  test: {
    environment: 'jsdom',
    include: ['frontend/**/*.test.js'],
  },
});
