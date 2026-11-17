/**
 * List of available MIME types and their corresponding file extensions
 */

const mimeTypes = new Map<string, string>([
  ["application/pdf", "pdf"],
  ["image/jpeg", "jpg"],
  ["image/png", "png"],
  ["image/webp", "webp"],
]);

/**
 * Function to convert a base64 string to a File
 */

export function convertBase64ToFile(base64String: string, filenameWithoutExt: string): File {
  const [dataUrlHeader, base64Payload] = base64String.split(",");
  if (dataUrlHeader === undefined || base64Payload === undefined) {
    throw new Error(`Base64 string must be a data URL of the form "data:<mime-type>;base64,<payload>"`);
  }

  // extract mime type
  const mimeType = /:(.*?);/.exec(dataUrlHeader)?.[1];
  if (!mimeType) {
    throw new Error("Failed to determine MIME type from base64 string");
  }

  // get file extension from MIME type
  const extension = mimeTypes.get(mimeType);
  if (!extension) {
    throw new Error(`MIME type "${mimeType}" is currently not supported. You can add it to the mimeTypes map.`);
  }

  const filename = `${filenameWithoutExt}.${extension}`;

  const bstr = atob(base64Payload); // Decode Base64
  let n = bstr.length;
  const u8arr = new Uint8Array(n);

  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }

  return new File([u8arr], filename, { type: mimeType });
}
