"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { uploadApi } from "@/lib/api/upload";
import {
  validateImageFile,
  type PresignedUrlResponse,
  type UploadQueueItem,
  type UploadSlot,
} from "../types";

export interface UseUploadQueueOptions {
  value: string[];
  onChange: (urls: string[]) => void;
  maxFiles: number;
  maxSize: number;
  presignFn: (file: File) => Promise<PresignedUrlResponse>;
}

export interface UseUploadQueueResult {
  slots: UploadSlot[];
  isUploading: boolean;
  addFiles: (files: FileList | File[]) => void;
  removeUpload: (id: string) => void;
  retryUpload: (id: string) => void;
  removeUrl: (url: string) => void;
  promoteUrl: (url: string) => void;
  clear: () => void;
}

function createId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `upload-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function urlsFromSlots(slots: UploadSlot[]): string[] {
  return slots
    .filter((slot): slot is Extract<UploadSlot, { kind: "url" }> => slot.kind === "url")
    .map((slot) => slot.url);
}

function buildUrlSlots(urls: string[]): UploadSlot[] {
  return urls.map((url, index) => ({
    kind: "url",
    key: `value-${index}-${url}`,
    url,
  }));
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Upload failed. Please try again.";
}

/**
 * Ordered upload queue.
 *
 * Every accepted file becomes a slot in insertion order, so the emitted URL
 * list stays deterministic (first selected image remains the cover image)
 * even when uploads finish out of order.
 */
export function useUploadQueue({
  value,
  onChange,
  maxFiles,
  maxSize,
  presignFn,
}: UseUploadQueueOptions): UseUploadQueueResult {
  const [slots, setSlots] = useState<UploadSlot[]>(() => buildUrlSlots(value));
  const slotsRef = useRef<UploadSlot[]>(slots);
  const controllersRef = useRef<Map<string, AbortController>>(new Map());
  const previewsRef = useRef<Map<string, string>>(new Map());
  const filesRef = useRef<Map<string, File>>(new Map());
  const onChangeRef = useRef(onChange);
  const presignRef = useRef(presignFn);
  const lastEmittedRef = useRef<string>(JSON.stringify(value));

  useEffect(() => {
    onChangeRef.current = onChange;
    presignRef.current = presignFn;
  }, [onChange, presignFn]);

  const applySlots = useCallback(
    (next: UploadSlot[] | ((prev: UploadSlot[]) => UploadSlot[])) => {
      const resolved =
        typeof next === "function" ? next(slotsRef.current) : next;
      slotsRef.current = resolved;
      setSlots(resolved);
    },
    []
  );

  const patchItem = useCallback(
    (id: string, patch: Partial<UploadQueueItem>) => {
      applySlots((prev) =>
        prev.map((slot) =>
          slot.kind === "upload" && slot.key === id
            ? { ...slot, item: { ...slot.item, ...patch } }
            : slot
        )
      );
    },
    [applySlots]
  );

  const startUpload = useCallback(
    async (id: string, file: File) => {
      const controller = new AbortController();
      controllersRef.current.set(id, controller);
      patchItem(id, {
        status: "uploading",
        progress: 0,
        errorMessage: undefined,
      });

      try {
        const presigned = await presignRef.current(file);
        if (controller.signal.aborted) return;

        await uploadApi.putBinaryToR2(presigned.uploadUrl, file, {
          signal: controller.signal,
          onProgress: (percent) => patchItem(id, { progress: percent }),
        });

        if (controller.signal.aborted) return;

        const previewUrl = previewsRef.current.get(id);
        if (previewUrl) {
          URL.revokeObjectURL(previewUrl);
          previewsRef.current.delete(id);
        }
        filesRef.current.delete(id);

        applySlots((prev) =>
          prev.map((slot) =>
            slot.kind === "upload" && slot.key === id
              ? { kind: "url", key: id, url: presigned.publicUrl }
              : slot
          )
        );
      } catch (error) {
        if (
          controller.signal.aborted ||
          (error as { code?: string })?.code === "ERR_CANCELED"
        ) {
          return;
        }
        patchItem(id, {
          status: "error",
          errorMessage: getErrorMessage(error),
        });
      } finally {
        controllersRef.current.delete(id);
      }
    },
    [applySlots, patchItem]
  );

  const addFiles = useCallback(
    (files: FileList | File[]) => {
      const incoming = Array.from(files);
      if (incoming.length === 0) return;

      const current = slotsRef.current;
      const available = Math.max(0, maxFiles - current.length);
      if (available === 0) return;

      const next = [...current];
      const queued: Array<{ id: string; file: File }> = [];

      for (const file of incoming.slice(0, available)) {
        const id = createId();
        const previewUrl = URL.createObjectURL(file);
        previewsRef.current.set(id, previewUrl);
        filesRef.current.set(id, file);

        const errorMessage = validateImageFile(file, maxSize);
        const item: UploadQueueItem = {
          id,
          file,
          previewUrl,
          status: errorMessage ? "error" : "idle",
          progress: 0,
          errorMessage: errorMessage ?? undefined,
        };

        next.push({ kind: "upload", key: id, item });
        if (!errorMessage) {
          queued.push({ id, file });
        }
      }

      applySlots(next);
      queued.forEach(({ id, file }) => {
        void startUpload(id, file);
      });
    },
    [applySlots, maxFiles, maxSize, startUpload]
  );

  const removeUpload = useCallback(
    (id: string) => {
      controllersRef.current.get(id)?.abort();
      controllersRef.current.delete(id);

      const previewUrl = previewsRef.current.get(id);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        previewsRef.current.delete(id);
      }
      filesRef.current.delete(id);

      applySlots((prev) => prev.filter((slot) => slot.key !== id));
    },
    [applySlots]
  );

  const retryUpload = useCallback(
    (id: string) => {
      const file = filesRef.current.get(id);
      if (!file) return;

      const validationError = validateImageFile(file, maxSize);
      if (validationError) {
        patchItem(id, { status: "error", errorMessage: validationError });
        return;
      }

      void startUpload(id, file);
    },
    [maxSize, patchItem, startUpload]
  );

  const removeUrl = useCallback(
    (url: string) => {
      applySlots((prev) => {
        const index = prev.findIndex(
          (slot) => slot.kind === "url" && slot.url === url
        );
        if (index === -1) return prev;
        return [...prev.slice(0, index), ...prev.slice(index + 1)];
      });
    },
    [applySlots]
  );

  const promoteUrl = useCallback(
    (url: string) => {
      applySlots((prev) => {
        const index = prev.findIndex(
          (slot) => slot.kind === "url" && slot.url === url
        );
        if (index <= 0) return prev;
        const target = prev[index];
        return [target, ...prev.slice(0, index), ...prev.slice(index + 1)];
      });
    },
    [applySlots]
  );

  const clear = useCallback(() => {
    controllersRef.current.forEach((controller) => controller.abort());
    controllersRef.current.clear();
    previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
    previewsRef.current.clear();
    filesRef.current.clear();
    applySlots([]);
  }, [applySlots]);

  // Keep the controlled value in sync when the parent resets/reloads the form.
  const valueKey = JSON.stringify(value);
  useEffect(() => {
    if (valueKey === lastEmittedRef.current) return;
    const hasActiveUpload = slotsRef.current.some(
      (slot) => slot.kind === "upload" && slot.item.status === "uploading"
    );
    if (hasActiveUpload) return;

    applySlots(buildUrlSlots(value));
    lastEmittedRef.current = valueKey;
    // value is represented by its serialized form to avoid identity churn
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [valueKey, applySlots]);

  // Emit the ordered URL list whenever committed slots change.
  useEffect(() => {
    const urls = urlsFromSlots(slots);
    const key = JSON.stringify(urls);
    if (key === lastEmittedRef.current) return;
    lastEmittedRef.current = key;
    onChangeRef.current(urls);
  }, [slots]);

  // Abort in-flight uploads and release object URLs on unmount.
  useEffect(() => {
    const controllers = controllersRef.current;
    const previews = previewsRef.current;
    return () => {
      controllers.forEach((controller) => controller.abort());
      controllers.clear();
      previews.forEach((url) => URL.revokeObjectURL(url));
      previews.clear();
    };
  }, []);

  const isUploading = slots.some(
    (slot) => slot.kind === "upload" && slot.item.status === "uploading"
  );

  return {
    slots,
    isUploading,
    addFiles,
    removeUpload,
    retryUpload,
    removeUrl,
    promoteUrl,
    clear,
  };
}
