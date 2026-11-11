function attachmentType(exam, blob) {
  const declaredType = String(exam.attachment?.contentType || blob.type || '').toLowerCase()
  const name = String(exam.attachment?.name || '').toLowerCase()
  if (declaredType === 'application/pdf' || name.endsWith('.pdf')) return 'application/pdf'
  if (declaredType === 'image/jpeg' || name.endsWith('.jpg') || name.endsWith('.jpeg')) return 'image/jpeg'
  if (declaredType === 'image/png' || name.endsWith('.png')) return 'image/png'
  return ''
}

export async function createCombinedExamPdf(exams, fetchAttachment) {
  if (!Array.isArray(exams) || exams.length === 0) throw new Error('Selecione ao menos um exame com arquivo.')
  const { PDFDocument } = await import('pdf-lib')
  const combined = await PDFDocument.create()

  for (const exam of exams) {
    const fileName = exam.attachment?.name || exam.name || exam.title || 'Exame'
    try {
      const blob = await fetchAttachment(exam)
      if (!blob || typeof blob.arrayBuffer !== 'function') throw new Error('O anexo não retornou um arquivo válido.')
      const type = attachmentType(exam, blob)
      const bytes = new Uint8Array(await blob.arrayBuffer())
      if (type === 'application/pdf') {
        const source = await PDFDocument.load(bytes)
        const pages = await combined.copyPages(source, source.getPageIndices())
        pages.forEach((page) => combined.addPage(page))
      } else if (type === 'image/jpeg' || type === 'image/png') {
        const image = type === 'image/jpeg' ? await combined.embedJpg(bytes) : await combined.embedPng(bytes)
        const page = combined.addPage([612, 792])
        const dimensions = image.scaleToFit(540, 720)
        page.drawImage(image, {
          x: (612 - dimensions.width) / 2,
          y: (792 - dimensions.height) / 2,
          ...dimensions,
        })
      } else {
        throw new Error('Formato não suportado. Use arquivos PDF, JPG ou PNG.')
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      throw new Error(`Não foi possível incluir "${fileName}" no PDF: ${message}`)
    }
  }

  if (combined.getPageCount() === 0) throw new Error('Os exames selecionados não contêm páginas para exportar.')
  return combined.save()
}
