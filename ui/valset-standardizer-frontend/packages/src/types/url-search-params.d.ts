export {};

declare global {
  interface URLSearchParams {
    append(name: string, value: any): void;
  }
}
