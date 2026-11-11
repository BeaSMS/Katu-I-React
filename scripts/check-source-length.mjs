import { readdir, readFile } from 'node:fs/promises'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseAst } from 'rolldown/parseAst'

const raiz = fileURLToPath(new URL('../', import.meta.url))
const extensoes = new Set(['.js', '.jsx', '.mjs', '.cjs', '.ts', '.tsx', '.css'])
const ignorados = new Set(['.git', 'node_modules', 'dist', 'release', 'build'])
const violacoes = []

async function inspecionar(diretorio) {
  for (const item of await readdir(diretorio, { withFileTypes: true })) {
    if (item.isDirectory() && ignorados.has(item.name)) continue
    const caminho = join(diretorio, item.name)
    if (item.isDirectory()) {
      await inspecionar(caminho)
      continue
    }
    if (!extensoes.has(item.name.slice(item.name.lastIndexOf('.')))) continue

    const codigoFonte = await readFile(caminho, 'utf8')
    const quantidadeLinhas = item.name.endsWith('.jsx')
      ? contarLinhasDeCodigo(codigoFonte, caminho)
      : contarLinhas(codigoFonte)
    if (quantidadeLinhas > 200) violacoes.push({ caminho: relative(raiz, caminho), quantidadeLinhas })
  }
}

function contarLinhas(codigoFonte) {
  return codigoFonte.split(/\r\n|\n|\r/).length - (/(?:\r\n|\n|\r)$/.test(codigoFonte) ? 1 : 0)
}

function contarLinhasDeCodigo(codigoFonte, nomeArquivo) {
  const arvoreSintatica = parseAst(codigoFonte, { lang: 'jsx', sourceType: 'module' }, nomeArquivo)
  const marcacao = new Uint8Array(codigoFonte.length)

  function marcarIntervalo(inicio, fim) {
    marcacao.fill(1, inicio, fim)
  }

  function visitarMarcacao(no) {
    if (!no || typeof no !== 'object') return

    if (no.type === 'JSXElement' || no.type === 'JSXFragment') {
      marcarIntervalo(no.start, no.end)
      return
    }

    if (Array.isArray(no)) {
      for (const filho of no) visitarMarcacao(filho)
      return
    }

    for (const valor of Object.values(no)) {
      if (valor && typeof valor === 'object') visitarMarcacao(valor)
    }
  }

  visitarMarcacao(arvoreSintatica)
  let quantidadeLinhas = 0
  let linhaTemCodigo = false

  for (let indice = 0; indice < codigoFonte.length; indice += 1) {
    if (codigoFonte[indice] === '\n' || codigoFonte[indice] === '\r') {
      if (linhaTemCodigo) quantidadeLinhas += 1
      linhaTemCodigo = false
      continue
    }
    if (!marcacao[indice] && !/\s/.test(codigoFonte[indice])) linhaTemCodigo = true
  }

  if (linhaTemCodigo) quantidadeLinhas += 1
  return quantidadeLinhas
}

await inspecionar(raiz)
if (violacoes.length) {
  console.error('Arquivos de código acima do limite de 200 linhas:')
  for (const violacao of violacoes)
    console.error(`- ${violacao.caminho}: ${violacao.quantidadeLinhas} linhas de código`)
  process.exitCode = 1
} else {
  console.log('Arquitetura validada: todos os arquivos de código têm até 200 linhas.')
}
