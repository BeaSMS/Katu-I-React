package com.katui.offline.data.repository

import android.content.Context
import com.katui.offline.data.local.dao.AlarmeDao
import com.katui.offline.data.local.dao.MedicamentoDao
import com.katui.offline.data.local.dao.UsuarioDao
import com.katui.offline.data.local.entities.*
import com.katui.offline.util.AlarmScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmeRepository @Inject constructor(
    val alarmeDao: AlarmeDao,
    private val usuarioDao: UsuarioDao,
    private val medicamentoDao: MedicamentoDao,
    private val anticoncepcionalDao: com.katui.offline.data.local.dao.AnticoncepcionalDao,
    @ApplicationContext private val context: Context
) {
    fun getAlarmes(usuarioId: Long): Flow<List<Alarme>> =
        alarmeDao.getAlarmesByUsuario(usuarioId)

    suspend fun insertAlarme(alarme: Alarme): Long {
        val id = alarmeDao.insert(alarme)
        
        val usuario = usuarioDao.getUsuario(alarme.usuarioId)
        val medicamento = alarme.medicamentoId?.let { medicamentoDao.getMedicamento(it) }
        
        val userName = usuario?.nome ?: "Paciente"
        val dosage = medicamento?.dosagem ?: ""
        val sexo = usuario?.sexo ?: SexoBiologico.NAO_INFORMADO
        
        AlarmScheduler.schedule(
            context, 
            id, 
            alarme.titulo, 
            alarme.horario, 
            userName, 
            dosage, 
            sexo, 
            isCustomAlarm = alarme.medicamentoId == null,
            userId = alarme.usuarioId
        )
        return id
    }

    suspend fun getAlarmesPendentes(): List<Alarme> = alarmeDao.getAlarmesPendentes()

    suspend fun getUsuarioNome(usuarioId: Long): String = usuarioDao.getUsuario(usuarioId)?.nome ?: "Paciente"

    suspend fun getUsuarioSexo(usuarioId: Long): SexoBiologico = usuarioDao.getUsuario(usuarioId)?.sexo ?: SexoBiologico.NAO_INFORMADO

    suspend fun getMedicamentoDosagem(medicamentoId: Long): String = medicamentoDao.getMedicamento(medicamentoId)?.dosagem ?: ""

    suspend fun updateStatus(id: Long, tomado: Boolean) {
        val alarme = alarmeDao.getAlarmeById(id) ?: return
        
        // Só processa se o status realmente mudou
        if (alarme.tomado == tomado) return

        alarmeDao.updateStatus(id, tomado)

        if (tomado) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(com.katui.offline.util.AlarmScheduler.safeRequestCode(id))
            
            // Subtração automática de estoque e Alerta de estoque baixo
            alarme.medicamentoId?.let { medId ->
                val med = medicamentoDao.getMedicamento(medId)
                med?.let {
                    // Sync com Saúde Feminina se for anticoncepcional
                    if (it.finalidade == "Anticoncepcional") {
                        val anti = anticoncepcionalDao.getAnticoncepcionalAtivo(it.usuarioId).first()
                        anti?.let { a ->
                            anticoncepcionalDao.insertRegistro(RegistroAnticoncepcional(
                                anticoncepcionalId = a.id,
                                data = alarme.horario.toLocalDate(),
                                tomado = true
                            ))
                        }
                    }

                    if (it.estoqueAtual > 0) {
                        val novoEstoque = (it.estoqueAtual - it.dosePorTomada).coerceAtLeast(0.0)
                        medicamentoDao.subtrairEstoque(it.id, it.dosePorTomada)
                        
                        // Dispara notificação de estoque baixo se necessário
                        if (novoEstoque <= it.estoqueMinimo) {
                            showLowStockNotification(it.nome, novoEstoque, it.unidadeMedida)
                        }
                    }
                }
            }
        }
    }

    private fun showLowStockNotification(nome: String, estoqueRestante: Double, unidade: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "katui_stock_alerts"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, 
                "Alertas de Estoque", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.katui.offline.R.drawable.ic_notification_custom)
            .setContentTitle("Estoque Baixo: $nome")
            .setContentText("Restam apenas ${if (estoqueRestante % 1.0 == 0.0) estoqueRestante.toInt() else estoqueRestante} $unidade.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(nome.hashCode(), notification)
    }

    suspend fun deleteAlarme(alarme: Alarme) {
        alarmeDao.delete(alarme)
        AlarmScheduler.cancel(context, alarme.id)
    }

    // Cancela todos os alarmes pendentes de um medicamento (usados ao editar o remédio)
    suspend fun deletePendentesDoMedicamento(medicamentoId: Long) {
        val pendentes = alarmeDao.getAlarmesPendentesByMedicamento(medicamentoId)
        pendentes.forEach { alarme ->
            alarmeDao.delete(alarme)
            AlarmScheduler.cancel(context, alarme.id)
        }
    }

    // Usado quando o usuário escolhe excluir "todos os futuros" a partir de um
    // alarme específico de um medicamento na tela de Alarmes (em vez de excluir
    // só aquela ocorráncia do dia, que é o que delete(alarme) já faz). Cancela o
    // AlarmManager de cada um antes de apagar do banco.
    suspend fun deleteFuturosDoMedicamento(medicamentoId: Long) {
        val futuros = alarmeDao.getAlarmesFuturosByMedicamento(medicamentoId, LocalDateTime.now())
        futuros.forEach { alarme ->
            alarmeDao.delete(alarme)
            AlarmScheduler.cancel(context, alarme.id)
        }
    }

    suspend fun generateAlarmsForMedication(med: Medicamento) {
        val startDateTime = try {
            LocalDateTime.of(med.dataInicio ?: java.time.LocalDate.now(), LocalTime.parse(med.horario))
        } catch (e: Exception) {
            LocalDateTime.now().withHour(8).withMinute(0)
        }

        val calculatedEnd = med.dataFim?.atTime(23, 59)
            ?: startDateTime.plusDays((med.dias?.toLong() ?: 30L))
        
        val windowLimit = LocalDateTime.now().plusDays(30)
        val endDateTime = if (calculatedEnd.isAfter(windowLimit)) windowLimit else calculatedEnd

        val intervalHours = when (med.tipoFrequencia) {
            "INTERVALO_HORAS" -> med.valorFrequencia?.toLong() ?: 8L
            "VEZES_DIA" -> 24L / (med.valorFrequencia ?: 1).coerceAtLeast(1)
            else -> 24L
        }.coerceAtLeast(1L)

        var alarmCount = 0
        val now = LocalDateTime.now()

        var current = if (startDateTime.isAfter(now)) {
            startDateTime
        } else {
            val hoursSinceStart = java.time.Duration.between(startDateTime, now).toHours()
            val stepsToSkip = (hoursSinceStart / intervalHours) + 1
            startDateTime.plusHours(stepsToSkip * intervalHours)
        }

        while (current.isBefore(endDateTime) || current.isEqual(endDateTime)) {
            if (current.isAfter(now)) {
                insertAlarme(
                    Alarme(
                        usuarioId = med.usuarioId,
                        medicamentoId = med.id,
                        horario = current,
                        titulo = "Tomar ${med.nome} (${med.dosagem ?: ""})"
                    )
                )
                alarmCount++
            }
            if (alarmCount > 200) break
            current = current.plusHours(intervalHours)
        }
    }
}
