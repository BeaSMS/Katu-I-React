package com.katui.offline.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.katui.offline.data.local.dao.*
import com.katui.offline.data.local.entities.*

@Database(
    entities = [
        Usuario::class, Medicamento::class, Alarme::class, Sintoma::class,
        Consulta::class, Exame::class, ExameResultado::class, Receita::class, CuidadorPacienteCrossRef::class,
        Vacina::class, CicloMenstrual::class, SintomaDiarioFeminino::class,
        Anticoncepcional::class, RegistroAnticoncepcional::class, Vitals::class,
        MedicamentoCatalogo::class, SintomaCatalogo::class, EfeitoColateralCatalogo::class,
        SaudeItemCatalogo::class, Gestacao::class, ConsultaLembrete::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KatuiDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun sintomaDao(): SintomaDao
    abstract fun consultaDao(): ConsultaDao
    abstract fun consultaLembreteDao(): ConsultaLembreteDao
    abstract fun alarmeDao(): AlarmeDao
    abstract fun exameDao(): ExameDao
    abstract fun receitaDao(): ReceitaDao
    abstract fun vacinaDao(): VacinaDao
    abstract fun cicloMenstrualDao(): CicloMenstrualDao
    abstract fun sintomaDiarioFemininoDao(): SintomaDiarioFemininoDao
    abstract fun anticoncepcionalDao(): AnticoncepcionalDao
    abstract fun vitalsDao(): VitalsDao
    abstract fun gestacaoDao(): GestacaoDao

    abstract fun medicamentoCatalogoDao(): MedicamentoCatalogoDao
    abstract fun sintomaCatalogoDao(): SintomaCatalogoDao
    abstract fun saudeItemCatalogoDao(): SaudeItemCatalogoDao
}
