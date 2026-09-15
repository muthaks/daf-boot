/**
 * Runtime configuration of this remote, read from assets/config.json relative to
 * the remote bundle (never the hosting shell's location). The container renders
 * the file from DAF_API_URL at start (nginx/default.conf.template).
 */
export interface DafConfig {
  /**
   * Absolute prefix of the DAF (approvalforum) API as the browser reaches it: the API gateway's
   * /daf route, which maps to <DAF server>/approvalforum/api. Requests are addressed as
   * /auth/... and /approvalForum/... under it.
   */
  apiUrl: string;
}

let configuration: Promise<DafConfig> | undefined;

function absoluteHttpUrl(value: string | undefined, name: string): string {
  const url = new URL(value ?? "");
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash
  ) {
    throw new Error(`${name} must be an absolute HTTP(S) URL.`);
  }
  return url.href.replace(/\/$/, "");
}

export function loadDafConfig(): Promise<DafConfig> {
  return (configuration ??= fetch(new URL("./assets/config.json", import.meta.url), {
    signal: AbortSignal.timeout(10000),
  })
    .then(async (response) => {
      if (!response.ok) throw new Error("Committee Agendas configuration is unavailable.");
      const config: Partial<DafConfig> = await response.json();
      return { apiUrl: absoluteHttpUrl(config.apiUrl, "apiUrl") };
    })
    .catch((error) => {
      configuration = undefined;
      throw error;
    }));
}
