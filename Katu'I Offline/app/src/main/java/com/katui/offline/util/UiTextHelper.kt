package com.katui.offline.util

import androidx.compose.ui.graphics.Color
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.ui.theme.AccentBlue
import com.katui.offline.ui.theme.AccentOrange
import com.katui.offline.ui.theme.AccentPink
import com.katui.offline.ui.theme.AccentPurple
import com.katui.offline.ui.theme.AccentTeal
import com.katui.offline.ui.theme.CorPrincipal

object UiTextHelper {
    
    /**
     * Retorna uma cor baseada na escolha do usuário ou determinística como fallback.
     */
    fun getUserColor(usuario: Usuario?): Color {
        if (usuario == null) return CorPrincipal
        
        // Se o usuário escolheu uma cor, usa ela
        usuario.corHex?.let { hex ->
            try {
                return Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                // Fallback se o hex for inválido
            }
        }
        
        // Se não escolheu, tentamos a determinística baseada no ID,
        // mas o primeiro perfil (ID 1) deve ser sempre o Verde original se não editado
        if (usuario.id == 1L) return CorPrincipal

        // Cores vibrantes padrão
        val palette = listOf(
            AccentBlue, 
            AccentTeal, 
            AccentOrange, 
            AccentPurple, 
            AccentPink,
            Color(0xFF4CAF50), // Verde
            Color(0xFFFFC107), // Amarelo/Âmbar
            Color(0xFF3F51B5), // Indigo
            Color(0xFF9C27B0), // Deep Purple
            Color(0xFF00BCD4)  // Cyan
        )
        
        val index = (usuario.id % palette.size).toInt()
        return palette[index]
    }

    /**
     * Paleta de cores sugerida para escolha de perfil (sem duplicatas).
     */
    fun getProfilePalette(): List<Color> = listOf(
        AccentBlue,        // #3F51B5
        AccentTeal,        // #009688
        AccentOrange,      // #FF9800
        AccentPurple,      // #673AB7
        AccentPink,        // #E91E63
        Color(0xFF4CAF50), // Verde
        Color(0xFFFFC107), // Amarelo/Âmbar
        Color(0xFF9C27B0), // Deep Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF9E9E9E), // Grey
        Color(0xFF607D8B)  // Blue Grey
    )

    /**
     * Converte Color para String Hex.
     */
    fun colorToHex(color: Color): String {
        return String.format("#%02X%02X%02X", 
            (color.red * 255).toInt(), 
            (color.green * 255).toInt(), 
            (color.blue * 255).toInt()
        )
    }
    
    /**
     * Retorna a preposição correta ("do"/"da") baseada no sexo biológico do usuário.
     * Usado para evitar "de" genérico onde o gênero é conhecido.
     */
    fun getPreposicao(usuario: Usuario?): String {
        return when (usuario?.sexo) {
            SexoBiologico.FEMININO, SexoBiologico.INTERSEXO -> "da"
            else -> "do"
        }
    }

    /**
     * Retorna o texto formatado dependendo se o usuário é o próprio ou um dependente.
     * 
     * @param usuario O usuário atual selecionado.
     * @param paraMim Texto usado quando o usuário é o principal (ex: "Meus Remédios")
     * @param paraDependenteMasc Texto para dependente masculino/não informado, com placeholder %s para o nome (ex: "Remédios do %s")
     * @param paraDependenteFem Texto para dependente feminino/intersexo, com placeholder %s para o nome (ex: "Remédios da %s"). Se null, usa paraDependenteMasc.
     */
    fun format(usuario: Usuario?, paraMim: String, paraDependenteMasc: String, paraDependenteFem: String? = null): String {
        if (usuario == null) return paraMim
        
        return if (usuario.tipo == TipoUsuario.CUIDADOR && usuario.cuidadorId == null) {
            paraMim
        } else {
            val primeiroNome = usuario.nome.split(" ").firstOrNull() ?: ""
            // FIX: aplica o template correto por gênero se o caller forneceu os dois;
            // caso contrário usa o template único (comportamento anterior preservado).
            val template = when (usuario.sexo) {
                SexoBiologico.FEMININO, SexoBiologico.INTERSEXO -> paraDependenteFem ?: paraDependenteMasc
                else -> paraDependenteMasc
            }
            template.format(primeiroNome)
        }
    }

    /**
     * Retorna a saudação apropriada.
     */
    fun getGreeting(usuario: Usuario?): String {
        val nome = usuario?.nome?.split(" ")?.firstOrNull() ?: ""
        return if (usuario?.tipo == TipoUsuario.CUIDADOR && usuario.cuidadorId == null) {
            "Olá, $nome!"
        } else {
            // FIX: antes usava sempre "de" independente do gênero, gerando "Perfil de Julia"
            // em vez de "Perfil da Julia". Agora usa a preposição correta baseada no sexo.
            val preposicao = when (usuario?.sexo) {
                SexoBiologico.FEMININO, SexoBiologico.INTERSEXO -> "da"
                else -> "do"
            }
            "Perfil $preposicao $nome"
        }
    }

    /**
     * Retorna a pergunta de bem-estar apropriada.
     */
    fun getFeelingQuestion(usuario: Usuario?): String {
        val nome = usuario?.nome?.split(" ")?.firstOrNull() ?: ""
        return if (usuario?.tipo == TipoUsuario.CUIDADOR && usuario.cuidadorId == null) {
            "Como você está se sentindo hoje?"
        } else {
            "Como $nome está se sentindo hoje?"
        }
    }
}
