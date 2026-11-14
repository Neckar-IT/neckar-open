/**
 * Function to convert a File to a base64 string
 *
 * @returns the base64 string with data prefix (e.g. `data:image/png;base64,...`) to use as src for an img tag
 */

export function convertFileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}
