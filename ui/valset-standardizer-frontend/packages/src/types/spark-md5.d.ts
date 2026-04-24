declare module "spark-md5" {
  class SparkMD5ArrayBuffer {
    append(chunk: ArrayBuffer): SparkMD5ArrayBuffer;
    end(raw?: boolean): string;
    reset(): SparkMD5ArrayBuffer;
    static hash(chunk: ArrayBuffer, raw?: boolean): string;
  }

  class SparkMD5 {
    static ArrayBuffer: typeof SparkMD5ArrayBuffer;
    static hash(content: string, raw?: boolean): string;
    static hashBinary(content: string, raw?: boolean): string;
  }

  export default SparkMD5;
}
