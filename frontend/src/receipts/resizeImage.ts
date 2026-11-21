const MAX_SIDE = 1600
const JPEG_QUALITY = 0.85

/**
 * Shrinks a phone photo (often 4000px, 3-8 MB) to at most 1600px before uploading.
 * Text on a receipt stays readable, the upload is much faster on mobile data, it stays well under
 * the 5 MB server limit, and the vision model is cheaper (cost grows with image size).
 * If the browser cannot decode the file, the original is sent and the server decides.
 */
export async function resizeImage(file: File): Promise<Blob> {
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY))
    return blob ?? file
  } catch {
    return file
  }
}
