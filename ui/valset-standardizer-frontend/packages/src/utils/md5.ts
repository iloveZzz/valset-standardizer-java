import SparkMD5 from "spark-md5";

const READ_CHUNK_SIZE = 2 * 1024 * 1024;

export const calculateFileMD5 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    const spark = new SparkMD5.ArrayBuffer();
    let currentChunk = 0;
    const totalChunks = Math.max(1, Math.ceil(file.size / READ_CHUNK_SIZE));

    const loadNext = () => {
      const start = currentChunk * READ_CHUNK_SIZE;
      const end = Math.min(start + READ_CHUNK_SIZE, file.size);
      reader.readAsArrayBuffer(file.slice(start, end));
    };

    reader.onload = (event) => {
      if (event.target?.result) {
        spark.append(event.target.result as ArrayBuffer);
      }

      currentChunk += 1;
      if (currentChunk < totalChunks) {
        loadNext();
        return;
      }

      resolve(spark.end());
    };

    reader.onerror = () => {
      reject(new Error("文件读取失败"));
    };

    loadNext();
  });

export const calculateBlobMD5 = (blob: Blob): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    const spark = new SparkMD5.ArrayBuffer();

    reader.onload = (event) => {
      if (event.target?.result) {
        spark.append(event.target.result as ArrayBuffer);
      }
      resolve(spark.end());
    };

    reader.onerror = () => {
      reject(new Error("分片读取失败"));
    };

    reader.readAsArrayBuffer(blob);
  });
