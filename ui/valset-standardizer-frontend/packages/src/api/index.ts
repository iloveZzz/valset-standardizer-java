export * from "./generated/valset";
import { getJavaApi } from "./generated/valset";
import { customInstance } from "./mutator";
import type {
  SingleResultTransferSourceMutationResponse,
  UploadSourceFilesRequest,
} from "./generated/valset/schemas";

type UploadSourceFilesCompatRequest = Omit<
  UploadSourceFilesRequest,
  "file" | "files"
> & {
  file?: Blob | File | null;
  files?: Array<Blob | File> | Blob | File | null;
};

export const getJavaSpringBootQuartzApi = () => {
  const api = getJavaApi();
  return {
    ...api,
    redeliver1: api.redeliver,
    uploadSourceFiles: (
      sourceId: string,
      uploadSourceFilesRequest: UploadSourceFilesCompatRequest,
    ) => {
      const formData = new FormData();
      formData.append("sourceId", uploadSourceFilesRequest.sourceId);
      if (uploadSourceFilesRequest.file) {
        formData.append("file", uploadSourceFilesRequest.file);
      }
      const files = uploadSourceFilesRequest.files;
      if (Array.isArray(files)) {
        files.forEach((file) => formData.append("files", file));
      } else if (files) {
        formData.append("files", files);
      }
      return customInstance<SingleResultTransferSourceMutationResponse>({
        url: `/transfer-sources/${sourceId}/upload`,
        method: "POST",
        headers: { "Content-Type": "multipart/form-data" },
        data: formData,
      });
    },
  };
};
export {
  deleteEtlWorkflowDefinition,
  getEtlWorkflowDefinition,
  listEtlWorkflowDefinitions,
  listEtlWorkflowPlatforms,
  saveEtlWorkflowDefinition,
  runEtlWorkflowDefinition,
  syncEtlWorkflowDefinition,
  validateEtlWorkflowDefinition,
} from "./etlWorkflowConfig";
export {
  callbackEtlWorkflowInstance,
  getEtlWorkflowInstance,
  listEtlWorkflowInstanceLogs,
  listEtlWorkflowInstances,
  pauseEtlWorkflowInstance,
  resumeEtlWorkflowInstance,
  retryEtlWorkflowInstance,
  stopEtlWorkflowInstance,
  triggerEtlWorkflowInstance,
} from "./etlWorkflowInstance";
export * from "./etlWorkflowTaskInstance";
export * from "./systemOutputLog";
export * from "./transferDeliveryRecord";
export * from "./outsourcedDataTask";
export * from "./batchValuationTask";
export * from "./batchValuationTaskTrace";
export * from "./qlexpressFunction";
