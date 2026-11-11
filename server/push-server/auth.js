const authApiUrl = process.env.AUTH_API_URL

if (!authApiUrl) throw new Error('AUTH_API_URL must point to the authenticated API /me endpoint base.')

export async function authenticate(req, res, next) {
  const authorization = req.get('authorization') || ''
  if (!authorization.startsWith('Bearer ')) return res.status(401).json({ message: 'Entre na sua conta para gerenciar alarmes em segundo plano.' })
  const profileId = req.get('x-profile-id')
  if (!profileId) return res.status(400).json({ message: 'Selecione um perfil de cuidado para gerenciar os alarmes.' })

  try {
    const response = await fetch(`${authApiUrl.replace(/\/$/, '')}/me`, {
      headers: { Authorization: authorization, 'X-Profile-Id': profileId },
      signal: AbortSignal.timeout(5000),
    })
    if (!response.ok) return res.status(response.status === 401 ? 401 : 502).json({ message: 'Não foi possível validar sua sessão para notificações.' })
    const account = await response.json()
    const profiles = Array.isArray(account.profiles) ? account.profiles : []
    if (!profiles.some((profile) => String(profile.id) === String(profileId))) {
      return res.status(403).json({ message: 'Este perfil não pertence à conta autenticada.' })
    }
    const accountId = account.user?.id || account.userId || account.id
    if (!accountId) return res.status(502).json({ message: 'A API de autenticação precisa retornar um ID estável da conta em /me.' })
    req.pushIdentity = {
      ownerId: String(accountId),
      profileId: String(profileId),
    }
    return next()
  } catch (error) {
    return res.status(502).json({ message: `Falha ao validar a sessão: ${error.message}` })
  }
}

