package com.katui.offline

import android.app.Application
import android.os.Build
import android.util.Log
import com.katui.offline.data.local.KatuiDatabase
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import com.katui.offline.data.repository.SaudeCatalogoRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KatuiApplication : Application() {

    @Inject
    lateinit var saudeRepository: SaudeCatalogoRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // Só inicializa catálogo se for o processo principal para economizar 
        // recursos nos processos secundários (alarm, sync)
        if (isMainProcess()) {
            inicializarSaudeCatalogo()
        }
    }

    private fun isMainProcess(): Boolean {
        val processName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                am.runningAppProcesses?.find { it.pid == android.os.Process.myPid() }?.processName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
        return processName == packageName
    }

    private fun inicializarSaudeCatalogo() {
        applicationScope.launch {
            try {
                val currentCount = saudeRepository.getCount()
                val hasTDAH = saudeRepository.search("TDAH").first().isNotEmpty()
                
                if (currentCount < 40 || !hasTDAH) {
                    Log.d("KATUI_DEBUG", "Populando catálogo global de saúde...")
                    val itens = mutableListOf<SaudeItemCatalogo>()

                    // --- CONDIÇÕES ---
                    itens.add(SaudeItemCatalogo(nome = "Hipertensão Arterial", categoria = "Cardiovascular", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Insuficiência Cardíaca", categoria = "Cardiovascular", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Doença Cardíaca Isquêmica", categoria = "Cardiovascular", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Sequela de AVC", categoria = "Cardiovascular", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "Diabetes Mellitus Tipo I", categoria = "Metabólica/Endócrina", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Diabetes Mellitus Tipo II", categoria = "Metabólica/Endócrina", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Obesidade (Grau III)", categoria = "Metabólica/Endócrina", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Colesterol Elevado (Dislipidemia)", categoria = "Metabólica/Endócrina", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Hipotireoidismo", categoria = "Metabólica/Endócrina", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "Asma", categoria = "Respiratória", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "DPOC (Bronquite/Enfisema)", categoria = "Respiratória", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Rinite Alérgica Crônica", categoria = "Respiratória", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "Epilepsia", categoria = "Neurológica", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Esclerose Múltipla", categoria = "Neurológica", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Doença de Parkinson", categoria = "Neurológica", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Doença de Alzheimer", categoria = "Neurológica", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "TDAH (Transtorno do Déficit de Atenção com Hiperatividade)", categoria = "Neurodesenvolvimento", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Autismo (TEA)", categoria = "Neurodesenvolvimento", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Dislexia", categoria = "Neurodesenvolvimento", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Síndrome de Tourette", categoria = "Neurodesenvolvimento", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "Depressão", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Ansiedade Generalizada (TAG)", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Transtorno de Pânico", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "TOC", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Esquizofrenia", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Transtorno Bipolar", categoria = "Saúde Mental", tipo = TipoSaudeItem.CONDICAO))

                    itens.add(SaudeItemCatalogo(nome = "Doença Renal Crônica", categoria = "Outras", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Doença Hepática Crônica", categoria = "Outras", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Lúpus / Autoimune", categoria = "Outras", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Neoplasia (Câncer)", categoria = "Outras", tipo = TipoSaudeItem.CONDICAO))
                    itens.add(SaudeItemCatalogo(nome = "Anemia Falciforme", categoria = "Outras", tipo = TipoSaudeItem.CONDICAO))

                    // --- ALERGIAS ---
                    itens.add(SaudeItemCatalogo(nome = "Dipirona", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Paracetamol", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Penicilinas (Amoxicilina, etc)", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Aspirina / AINEs", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Contraste Iodado", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Sulfa", categoria = "Medicamento", tipo = TipoSaudeItem.ALERGIA))

                    itens.add(SaudeItemCatalogo(nome = "Leite de Vaca (Lactose/APLV)", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Ovo", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Soja", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Trigo / Glúten", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Amendoim / Castanhas", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Frutos do Mar", categoria = "Alimentar", tipo = TipoSaudeItem.ALERGIA))

                    itens.add(SaudeItemCatalogo(nome = "Ácaros / Poeira", categoria = "Ambiental", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Pólen", categoria = "Ambiental", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Pelos de Animais (Cão/Gato)", categoria = "Ambiental", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Látex", categoria = "Outras Alergias", tipo = TipoSaudeItem.ALERGIA))
                    itens.add(SaudeItemCatalogo(nome = "Níquel / Bijuterias", categoria = "Outras Alergias", tipo = TipoSaudeItem.ALERGIA))

                    saudeRepository.insertAll(itens)
                }
            } catch (e: Exception) {
                Log.e("KATUI_DEBUG", "Erro ao inicializar catálogo", e)
            }
        }
    }
}
