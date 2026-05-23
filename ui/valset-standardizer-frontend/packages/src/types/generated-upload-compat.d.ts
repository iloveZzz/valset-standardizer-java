interface Blob {
  forEach: (
    callbackfn: (file: Blob, index: number, array: Blob[]) => void,
  ) => void;
}
