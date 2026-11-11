import assert from 'node:assert/strict'
import test from 'node:test'
import { PDFDocument } from 'pdf-lib'
import { createCombinedExamPdf } from '../src/services/examPdf.js'

test('combina páginas de exames PDF sem alterar a quantidade de páginas', async () => {
  const firstPdf = await PDFDocument.create()
  firstPdf.addPage()
  const secondPdf = await PDFDocument.create()
  secondPdf.addPage()
  secondPdf.addPage()
  const files = [await firstPdf.save(), await secondPdf.save()]
  const exams = files.map((_, index) => ({
    id: `exam-${index}`,
    attachment: { name: `exam-${index}.pdf`, contentType: 'application/pdf' },
  }))

  const combinedBytes = await createCombinedExamPdf(
    exams,
    async (exam) => new Blob([files[Number(exam.id.slice(-1))]], { type: 'application/pdf' }),
  )

  const combinedPdf = await PDFDocument.load(combinedBytes)
  assert.equal(combinedPdf.getPageCount(), 3)
})

test('recusa exportação sem exames selecionados', async () => {
  await assert.rejects(
    createCombinedExamPdf([], async () => new Blob()),
    /Selecione ao menos um exame/,
  )
})
