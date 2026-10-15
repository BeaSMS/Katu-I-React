package com.katui.offline.data.local.dao

import androidx.room.*
import com.katui.offline.data.local.entities.Alarme
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmeDao {
    @Query("SELECT * FROM alarmes WHERE usuarioId = :usuarioId ORDER BY horario ASC")
    fun getAlarmesByUsuario(usuarioId: Long): Flow<List<Alarme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarme: Alarme): Long

    @Query("SELECT * FROM alarmes WHERE id = :id")
    suspend fun getAlarmeById(id: Long): Alarme?

    @Query("UPDATE alarmes SET tomado = :tomado WHERE id = :id")
    suspend fun updateStatus(id: Long, tomado: Boolean)

    @Delete
    suspend fun delete(alarme: Alarme)

    // Busca alarmes futuros vinculados a um medicamento (para deletar antes de regenerar)
    @Query("SELECT * FROM alarmes WHERE medicamentoId = :medicamentoId AND tomado = 0")
    suspend fun getAlarmesPendentesByMedicamento(medicamentoId: Long): List<Alarme>

    // Usado quando o usuário escolhe excluir "todos os futuros" a partir de um
    // alarme específico na tela de Alarmes. Diferente de getAlarmesPendentesByMedicamento
    // (que também pega atrasados não marcados), este filtra por horário >= agora,
    // preservando o histórico de alarmes passados não tomados.
    @Query("SELECT * FROM alarmes WHERE medicamentoId = :medicamentoId AND horario >= :now")
    suspend fun getAlarmesFuturosByMedicamento(medicamentoId: Long, now: java.time.LocalDateTime): List<Alarme>

    // FIX: usado para reagendar todos os alarmes pendentes apos um reboot do
    // aparelho (o AlarmManager perde todos os alarmes agendados quando o
    // dispositivo reinicia). Busca de TODOS os usuarios (cuidador + dependentes),
    // nao apenas do usuario da sessao atual, pois qualquer perfil local pode
    // ter alarmes pendentes que precisam voltar a disparar.
    @Query("SELECT * FROM alarmes WHERE tomado = 0")
    suspend fun getAlarmesPendentes(): List<Alarme>
}
