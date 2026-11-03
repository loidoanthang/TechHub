/**
 * src/lib/security.ts
 * Security utility functions for TechHub Frontend
 */

/**
 * Validates whether a given URL is a strictly safe relative path for internal navigation,
 * preventing Open Redirect attacks (CWE-601).
 *
 * Checks:
 * 1. Must be a non-empty string starting with a single '/'
 * 2. Next character cannot be '/' or '\' (prevents protocol-relative // or backslash /\)
 * 3. Decoded URI must not contain control characters, raw backslashes, or schema indicators
 * 4. Must resolve to the exact same origin when parsed by the WHATWG URL standard
 */
export function isSafeRelativeUrl(url: string | null | undefined): boolean {
  if (!url || typeof url !== "string") return false;

  const trimmed = url.trim();
  if (!trimmed) return false;

  // Preliminary raw check
  if (!/^\/[^\/\\]/.test(trimmed)) {
    return false;
  }

  // Handle percent-encoded bypasses (%5C, %2F, %00, etc.)
  let decoded = trimmed;
  try {
    decoded = decodeURIComponent(trimmed);
  } catch {
    return false;
  }

  // Must still start with '/' and not followed by '/' or '\' after decode
  if (!/^\/[^\/\\]/.test(decoded)) {
    return false;
  }

  // Reject backslashes, control characters, or protocol schemes
  if (/[\x00-\x1F\x7F\\]/.test(decoded) || decoded.includes("://")) {
    return false;
  }

  try {
    const base = new URL("http://localhost");
    const parsed = new URL(decoded, base);
    return parsed.origin === base.origin && parsed.pathname.startsWith("/");
  } catch {
    return false;
  }
}
