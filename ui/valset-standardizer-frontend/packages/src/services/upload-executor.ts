import { getJavaSpringBootQuartzApi } from "@/api";

const generatedvalsetApi = getJavaSpringBootQuartzApi();

export interface UploadProgressPayload {
  uploadedBytes: number;
  totalBytes: number;
  uploadedChunks: number;
  totalChunks: number;
}

export interface UploadExecutorCallbacks {
  onProgress?: (taskId: string, payload: UploadProgressPayload) => void;
  onStatus?: (
    taskId: string,
    status: "checking" | "uploading" | "merging" | "completed",
  ) => void;
  onError?: (taskId: string, errorMessage: string) => void;
}

class UploadExecutor {
  private callbacks: UploadExecutorCallbacks | null = null;

  public setCallbacks(callbacks: UploadExecutorCallbacks) {
    this.callbacks = callbacks;
  }

  public async start(
    taskId: string,
    file: File,
    _chunkSize = 0,
    _concurrency = 1,
  ): Promise<void> {
    this.callbacks?.onStatus?.(taskId, "checking");
    this.callbacks?.onStatus?.(taskId, "uploading");
    this.callbacks?.onProgress?.(taskId, {
      uploadedBytes: 0,
      totalBytes: file.size,
      uploadedChunks: 0,
      totalChunks: 1,
    });

    try {
      await generatedvalsetApi.upload1(file);
      this.callbacks?.onProgress?.(taskId, {
        uploadedBytes: file.size,
        totalBytes: file.size,
        uploadedChunks: 1,
        totalChunks: 1,
      });
      this.callbacks?.onStatus?.(taskId, "merging");
      this.callbacks?.onStatus?.(taskId, "completed");
    } catch (error) {
      const message = error instanceof Error ? error.message : "文件上传失败";
      this.callbacks?.onError?.(taskId, message);
      throw new Error(message);
    }
  }
}

export const uploadExecutor = new UploadExecutor();
