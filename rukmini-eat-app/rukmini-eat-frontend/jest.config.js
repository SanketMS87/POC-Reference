/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testEnvironment: 'jsdom',
  testMatch: ['<rootDir>/src/**/*.spec.ts'],
  collectCoverageFrom: ['src/app/**/*.ts', '!src/app/**/*.spec.ts'],
  coverageDirectory: 'coverage',
  coverageReporters: ['text-summary', 'html', 'lcov'],
  coverageThreshold: {
    global: { statements: 80, branches: 70, functions: 80, lines: 80 }
  }
};
